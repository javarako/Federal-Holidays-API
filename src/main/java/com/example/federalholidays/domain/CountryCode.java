package com.example.federalholidays.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum CountryCode {
    CA("Canada"),
    US("United States");

    private final String displayName;

    CountryCode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static CountryCode fromPath(String value) {
        try {
            return CountryCode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnsupportedCountryException(value, supportedCodes());
        }
    }

    public static String supportedCodes() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
