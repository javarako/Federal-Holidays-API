package com.example.federalholidays.exception;

import com.example.federalholidays.entity.enumeration.CountryCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsUnsupportedCountryToNotFound() {
        var response = handler.handleUnsupportedCountry(new UnsupportedCountryException("MX", "CA, US"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Unsupported country 'MX'. Supported countries are CA, US.");
    }

    @Test
    void mapsHolidayNotFoundToNotFound() {
        var response = handler.handleNotFound(new HolidayNotFoundException(42L, CountryCode.CA));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Holiday 42 was not found for country CA.");
    }

    @Test
    void mapsUploadErrorsToBadRequest() {
        var response = handler.handleImport(new HolidayImportException("Upload file is required."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Upload file is required.");
    }

    @Test
    void mapsUnsupportedUploadTypesToUnsupportedMediaType() {
        var response = handler.handleUnsupportedUploadType(
                new UnsupportedHolidayFileTypeException("Unsupported upload file type. Use CSV or JSON.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().message()).isEqualTo("Unsupported upload file type. Use CSV or JSON.");
    }

    @Test
    void mapsDuplicateHolidayToConflict() {
        var response = handler.handleConflict();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Holiday already exists for that country, date, and name.");
    }

    @Test
    void mapsUnreadableRequestToBadRequest() {
        var response = handler.handleUnreadableRequest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid request data.");
    }
}
