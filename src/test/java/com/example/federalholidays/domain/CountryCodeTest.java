package com.example.federalholidays.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodeTest {

    @Test
    void parsesSupportedCountriesCaseInsensitively() {
        assertThat(CountryCode.fromPath("ca")).isEqualTo(CountryCode.CA);
        assertThat(CountryCode.fromPath("US")).isEqualTo(CountryCode.US);
    }

    @Test
    void rejectsUnsupportedCountries() {
        assertThatThrownBy(() -> CountryCode.fromPath("mx"))
                .isInstanceOf(UnsupportedCountryException.class)
                .hasMessageContaining("Supported countries are CA, US");
    }
}
