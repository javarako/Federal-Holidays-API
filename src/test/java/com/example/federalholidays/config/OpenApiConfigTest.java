package com.example.federalholidays.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void createsFederalHolidaysOpenApiMetadata() {
        OpenAPI openAPI = new OpenApiConfig().federalHolidaysOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Federal Holidays API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getInfo().getDescription())
                .isEqualTo("REST API for managing federal holidays for supported countries.");
    }
}
