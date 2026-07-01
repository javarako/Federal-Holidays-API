package com.example.federalholidays.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Holiday upload result")
public record HolidayImportResponse(
        @Schema(description = "Number of records found in the uploaded file", example = "10")
        int totalRecords,

        @Schema(description = "Number of records saved successfully", example = "9")
        int successfulRecords,

        @Schema(description = "Number of records rejected during validation", example = "1")
        int failedRecords,

        @Schema(description = "Row-level validation errors")
        List<UploadError> errors
) {
}
