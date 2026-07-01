package com.example.federalholidays.dto;

import com.example.federalholidays.entity.Holiday;
import com.example.federalholidays.entity.enumeration.CountryCode;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void mapsHolidayEntityToResponse() {
        Holiday holiday = new Holiday(CountryCode.CA, "Canada Day", LocalDate.of(2026, 7, 1), "National day");
        holiday.setId(11L);

        HolidayResponse response = HolidayResponse.from(holiday);

        assertThat(response).isEqualTo(new HolidayResponse(
                11L,
                "CA",
                "Canada Day",
                LocalDate.of(2026, 7, 1),
                "National day"
        ));
    }

    @Test
    void validatesHolidayRequestFields() {
        HolidayRequest request = new HolidayRequest("", null, "x".repeat(501));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString() + ": " + violation.getMessage())
                .contains(
                        "name: name is required",
                        "date: date is required",
                        "description: description must be at most 500 characters"
                );
    }

    @Test
    void representsUploadSummaryAndRowErrors() {
        HolidayImportResponse response = new HolidayImportResponse(
                2,
                1,
                1,
                List.of(new UploadError(2, "Invalid date format."))
        );

        assertThat(response.totalRecords()).isEqualTo(2);
        assertThat(response.successfulRecords()).isEqualTo(1);
        assertThat(response.failedRecords()).isEqualTo(1);
        assertThat(response.errors()).containsExactly(new UploadError(2, "Invalid date format."));
    }
}
