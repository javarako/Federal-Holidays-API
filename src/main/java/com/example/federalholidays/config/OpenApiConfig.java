package com.example.federalholidays.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI federalHolidaysOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Federal Holidays API")
                        .version("v1")
                        .description("REST API for managing federal holidays for supported countries."));
    }
}
