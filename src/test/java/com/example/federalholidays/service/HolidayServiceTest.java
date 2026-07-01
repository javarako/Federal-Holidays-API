package com.example.federalholidays.service;

import com.example.federalholidays.dto.HolidayImportResponse;
import com.example.federalholidays.dto.HolidayImportRow;
import com.example.federalholidays.dto.HolidayRequest;
import com.example.federalholidays.dto.HolidayResponse;
import com.example.federalholidays.entity.Holiday;
import com.example.federalholidays.entity.enumeration.CountryCode;
import com.example.federalholidays.exception.HolidayImportException;
import com.example.federalholidays.exception.HolidayNotFoundException;
import com.example.federalholidays.filter.HolidayListFilter;
import com.example.federalholidays.repo.HolidayRepository;
import com.example.federalholidays.util.HolidayFileParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private HolidayFileParser holidayFileParser;

    private HolidayService holidayService() {
        return new HolidayService(holidayRepository, holidayFileParser);
    }

    @Test
    void addsHolidayForCountry() {
        Holiday saved = holiday(CountryCode.CA, "Canada Day", LocalDate.of(2026, 7, 1), "National day");
        saved.setId(10L);
        when(holidayRepository.save(any(Holiday.class))).thenReturn(saved);

        HolidayResponse response = holidayService().addHoliday(
                CountryCode.CA,
                new HolidayRequest("Canada Day", LocalDate.of(2026, 7, 1), "National day")
        );

        assertThat(response).isEqualTo(new HolidayResponse(10L, "CA", "Canada Day", LocalDate.of(2026, 7, 1), "National day"));
    }

    @Test
    void updatesExistingHoliday() {
        Holiday existing = holiday(CountryCode.US, "Old Name", LocalDate.of(2026, 1, 1), null);
        existing.setId(4L);
        Holiday saved = holiday(CountryCode.US, "New Year", LocalDate.of(2026, 1, 1), "Updated");
        saved.setId(4L);
        when(holidayRepository.findByIdAndCountryCode(4L, CountryCode.US)).thenReturn(Optional.of(existing));
        when(holidayRepository.save(existing)).thenReturn(saved);

        HolidayResponse response = holidayService().updateHoliday(
                CountryCode.US,
                4L,
                new HolidayRequest("New Year", LocalDate.of(2026, 1, 1), "Updated")
        );

        assertThat(response.name()).isEqualTo("New Year");
        assertThat(existing.getName()).isEqualTo("New Year");
    }

    @Test
    void throwsWhenUpdatingMissingHoliday() {
        when(holidayRepository.findByIdAndCountryCode(99L, CountryCode.CA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService().updateHoliday(
                CountryCode.CA,
                99L,
                new HolidayRequest("Canada Day", LocalDate.of(2026, 7, 1), null)
        )).isInstanceOf(HolidayNotFoundException.class)
                .hasMessage("Holiday 99 was not found for country CA.");
    }

    @Test
    void listsHolidaysWithoutFilters() {
        when(holidayRepository.findByCountryCodeOrderByHolidayDateAscNameAsc(CountryCode.US))
                .thenReturn(List.of(
                        holiday(CountryCode.US, "Independence Day", LocalDate.of(2026, 7, 4), null),
                        holiday(CountryCode.US, "Thanksgiving Day", LocalDate.of(2026, 11, 26), null)
                ));

        List<HolidayResponse> response = holidayService().listHolidays(CountryCode.US, HolidayListFilter.empty());

        assertThat(response).extracting(HolidayResponse::name)
                .containsExactly("Independence Day", "Thanksgiving Day");
    }

    @Test
    void listsHolidaysWithYearFilter() {
        when(holidayRepository.findByCountryCodeAndHolidayDateBetweenOrderByHolidayDateAscNameAsc(
                eq(CountryCode.CA),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 12, 31))
        )).thenReturn(List.of(holiday(CountryCode.CA, "Canada Day", LocalDate.of(2026, 7, 1), null)));

        List<HolidayResponse> response = holidayService().listHolidays(
                CountryCode.CA,
                HolidayListFilter.from("2026", null, null)
        );

        assertThat(response).singleElement().extracting(HolidayResponse::name).isEqualTo("Canada Day");
    }

    @Test
    void rejectsMissingUploadFile() {
        assertThatThrownBy(() -> holidayService().importHolidays(CountryCode.CA, null))
                .isInstanceOf(HolidayImportException.class)
                .hasMessage("Upload file is required.");
    }

    @Test
    void importsValidRowsAndReportsValidationErrors() {
        MockMultipartFile file = csvFile("name,date,description\nCanada Day,2026-07-01,National day\n");
        when(holidayFileParser.parse(file)).thenReturn(List.of(
                new HolidayImportRow(1, "Canada Day", "2026-07-01", "National day"),
                new HolidayImportRow(2, "", "2026-09-07", null),
                new HolidayImportRow(3, "Invalid Date", "07/01/2026", null)
        ));
        when(holidayRepository.existsByCountryCodeAndHolidayDateAndName(CountryCode.CA, LocalDate.of(2026, 7, 1), "Canada Day"))
                .thenReturn(false);

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.totalRecords()).isEqualTo(3);
        assertThat(response.successfulRecords()).isEqualTo(1);
        assertThat(response.failedRecords()).isEqualTo(2);
        assertThat(response.errors()).extracting(error -> error.message())
                .containsExactly("name is required.", "Invalid date format.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Holiday>> holidaysCaptor = ArgumentCaptor.forClass(List.class);
        verify(holidayRepository).saveAll(holidaysCaptor.capture());
        assertThat(holidaysCaptor.getValue()).singleElement()
                .satisfies(holiday -> assertThat(holiday.getName()).isEqualTo("Canada Day"));
    }

    @Test
    void reportsDuplicateHolidayFromRepository() {
        MockMultipartFile file = csvFile("name,date,description\nCanada Day,2026-07-01,National day\n");
        when(holidayFileParser.parse(file)).thenReturn(List.of(
                new HolidayImportRow(1, "Canada Day", "2026-07-01", "National day")
        ));
        when(holidayRepository.existsByCountryCodeAndHolidayDateAndName(CountryCode.CA, LocalDate.of(2026, 7, 1), "Canada Day"))
                .thenReturn(true);

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.successfulRecords()).isZero();
        assertThat(response.failedRecords()).isEqualTo(1);
        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).isEqualTo("Duplicate holiday."));
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    void reportsDuplicateHolidayWithinUploadFile() {
        MockMultipartFile file = csvFile("name,date,description\nCanada Day,2026-07-01,National day\n");
        when(holidayFileParser.parse(file)).thenReturn(List.of(
                new HolidayImportRow(1, "Canada Day", "2026-07-01", "National day"),
                new HolidayImportRow(2, "Canada Day", "2026-07-01", "Duplicate")
        ));
        when(holidayRepository.existsByCountryCodeAndHolidayDateAndName(CountryCode.CA, LocalDate.of(2026, 7, 1), "Canada Day"))
                .thenReturn(false);

        HolidayImportResponse response = holidayService().importHolidays(CountryCode.CA, file);

        assertThat(response.successfulRecords()).isEqualTo(1);
        assertThat(response.failedRecords()).isEqualTo(1);
        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.row()).isEqualTo(2));
    }

    private Holiday holiday(CountryCode countryCode, String name, LocalDate date, String description) {
        return new Holiday(countryCode, name, date, description);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "holidays.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
