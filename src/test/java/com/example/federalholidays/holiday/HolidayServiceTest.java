package com.example.federalholidays.holiday;

import com.example.federalholidays.domain.CountryCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    private HolidayService holidayService;

    private HolidayService holidayService() {
        return new HolidayService(holidayRepository, new HolidayFileParser(new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    @Test
    void addsHolidayForCountry() {
        Holiday saved = new Holiday(CountryCode.CA, "Canada Day", LocalDate.of(2026, 7, 1), "National day");
        saved.setId(10L);
        when(holidayRepository.save(any(Holiday.class))).thenReturn(saved);

        HolidayResponse response = holidayService().addHoliday(
                CountryCode.CA,
                new HolidayRequest("Canada Day", LocalDate.of(2026, 7, 1), "National day")
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.countryCode()).isEqualTo("CA");
        assertThat(response.name()).isEqualTo("Canada Day");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void listsHolidaysForCountryOrderedByDate() {
        when(holidayRepository.findByCountryCodeOrderByHolidayDateAscNameAsc(CountryCode.US))
                .thenReturn(List.of(
                        new Holiday(CountryCode.US, "Independence Day", LocalDate.of(2026, 7, 4), null),
                        new Holiday(CountryCode.US, "Thanksgiving Day", LocalDate.of(2026, 11, 26), null)
                ));

        List<HolidayResponse> holidays = holidayService().listHolidays(CountryCode.US, HolidayListFilter.empty());

        assertThat(holidays).extracting(HolidayResponse::name)
                .containsExactly("Independence Day", "Thanksgiving Day");
    }

    @Test
    void listsHolidaysForCountryFilteredByYear() {
        when(holidayRepository.findByCountryCodeAndHolidayDateBetweenOrderByHolidayDateAscNameAsc(
                CountryCode.CA,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of(
                new Holiday(CountryCode.CA, "Canada Day", LocalDate.of(2026, 7, 1), null)
        ));

        List<HolidayResponse> holidays = holidayService().listHolidays(
                CountryCode.CA,
                HolidayListFilter.from("2026", null, null)
        );

        assertThat(holidays).extracting(HolidayResponse::name).containsExactly("Canada Day");
    }

    @Test
    void rejectsInvalidListFilterRange() {
        assertThatThrownBy(() -> HolidayListFilter.from(null, "2026-12-31", "2026-01-01"))
                .isInstanceOf(InvalidHolidayQueryException.class)
                .hasMessage("fromDate must be before or equal to toDate.");
    }

    @Test
    void updatesExistingHolidayWithoutChangingCountry() {
        Holiday existing = new Holiday(CountryCode.US, "Independence", LocalDate.of(2026, 7, 4), null);
        existing.setId(7L);
        when(holidayRepository.findByIdAndCountryCode(7L, CountryCode.US)).thenReturn(Optional.of(existing));
        when(holidayRepository.save(existing)).thenReturn(existing);

        HolidayResponse response = holidayService().updateHoliday(
                CountryCode.US,
                7L,
                new HolidayRequest("Independence Day", LocalDate.of(2026, 7, 3), "Observed")
        );

        assertThat(response.name()).isEqualTo("Independence Day");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(response.description()).isEqualTo("Observed");
        assertThat(existing.getCountryCode()).isEqualTo(CountryCode.US);
    }

    @Test
    void rejectsUpdatesForMissingHoliday() {
        when(holidayRepository.findByIdAndCountryCode(404L, CountryCode.CA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService().updateHoliday(
                CountryCode.CA,
                404L,
                new HolidayRequest("Canada Day", LocalDate.of(2026, 7, 1), null)
        )).isInstanceOf(HolidayNotFoundException.class);
    }

    @Test
    void importsCsvFileWithPartialSuccessSummary() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.csv",
                "text/csv",
                """
                name,date,description
                Canada Day,2026-07-01,National day
                ,2026-09-07,
                Thanksgiving,not-a-date,
                """.getBytes()
        );
        when(holidayRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.totalRecords()).isEqualTo(3);
        assertThat(response.successfulRecords()).isEqualTo(1);
        assertThat(response.failedRecords()).isEqualTo(2);
        assertThat(response.errors()).extracting(UploadError::row).containsExactly(2, 3);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<Holiday>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(holidayRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Holiday::getName)
                .containsExactly("Canada Day");
        assertThat(captor.getValue()).allMatch(holiday -> holiday.getCountryCode() == CountryCode.CA);
    }

    @Test
    void importsJsonFileAsHolidaysForCountry() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.json",
                "application/json",
                """
                [
                  {"name":"Canada Day","date":"2026-07-01","description":"National day"},
                  {"name":"Labour Day","date":"2026-09-07"}
                ]
                """.getBytes()
        );
        when(holidayRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.totalRecords()).isEqualTo(2);
        assertThat(response.successfulRecords()).isEqualTo(2);
        assertThat(response.failedRecords()).isZero();
    }

    @Test
    void reportsDuplicateHolidayUploadRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.csv",
                "text/csv",
                """
                name,date,description
                Canada Day,2026-07-01,National day
                """.getBytes()
        );
        when(holidayRepository.existsByCountryCodeAndHolidayDateAndName(
                CountryCode.CA,
                LocalDate.of(2026, 7, 1),
                "Canada Day"
        )).thenReturn(true);

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.totalRecords()).isEqualTo(1);
        assertThat(response.successfulRecords()).isZero();
        assertThat(response.failedRecords()).isEqualTo(1);
        assertThat(response.errors()).extracting(UploadError::message)
                .containsExactly("Duplicate holiday.");
    }

    @Test
    void rejectsUnsupportedUploadFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.txt",
                "text/plain",
                "Canada Day,2026-07-01".getBytes()
        );

        assertThatThrownBy(() -> holidayService().importHolidays(CountryCode.CA, file))
                .isInstanceOf(UnsupportedHolidayFileTypeException.class);
    }
}
