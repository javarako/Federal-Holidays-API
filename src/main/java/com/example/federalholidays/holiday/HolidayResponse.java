package com.example.federalholidays.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Holiday response")
public record HolidayResponse(
        @Schema(description = "Holiday identifier", example = "1")
        Long id,

        @Schema(description = "Country code", example = "CA", allowableValues = {"CA", "US"})
        String countryCode,

        @Schema(description = "Holiday name", example = "Canada Day")
        String name,

        @Schema(description = "Holiday date in ISO format", example = "2026-07-01")
        LocalDate date,

        @Schema(description = "Optional holiday description", example = "National day")
        String description
) {
    static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getCountryCode().name(),
                holiday.getName(),
                holiday.getHolidayDate(),
                holiday.getDescription()
        );
    }
}
