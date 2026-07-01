package com.example.federalholidays.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Row-level upload validation error")
public record UploadError(
        @Schema(description = "One-based row number in the uploaded file", example = "4")
        int row,

        @Schema(description = "Validation or parsing error for the row", example = "Invalid date format.")
        String message
) {
}
