package com.example.federalholidays.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Holiday create or update request")
public record HolidayRequest(
        @Schema(description = "Holiday name", example = "Canada Day", maxLength = 160)
        @NotBlank(message = "name is required")
        @Size(max = 160, message = "name must be at most 160 characters")
        String name,

        @Schema(description = "Holiday date in ISO format", example = "2026-07-01")
        @NotNull(message = "date is required")
        LocalDate date,

        @Schema(description = "Optional holiday description", example = "National day", maxLength = 500)
        @Size(max = 500, message = "description must be at most 500 characters")
        String description
) {
}
