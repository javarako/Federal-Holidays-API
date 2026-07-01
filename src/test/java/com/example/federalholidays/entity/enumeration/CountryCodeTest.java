package com.example.federalholidays.entity.enumeration;

import com.example.federalholidays.exception.UnsupportedCountryException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodeTest {

    @Test
    void parsesSupportedCountryCodesCaseInsensitively() {
        assertThat(CountryCode.fromPath("ca")).isEqualTo(CountryCode.CA);
        assertThat(CountryCode.fromPath("US")).isEqualTo(CountryCode.US);
    }

    @Test
    void rejectsUnsupportedCountryCode() {
        assertThatThrownBy(() -> CountryCode.fromPath("MX"))
                .isInstanceOf(UnsupportedCountryException.class)
                .hasMessage("Unsupported country 'MX'. Supported countries are CA, US.");
    }

    @Test
    void listsSupportedCountryCodes() {
        assertThat(CountryCode.supportedCodes()).isEqualTo("CA, US");
    }
}
