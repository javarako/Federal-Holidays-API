package com.example.federalholidays.exception;

public class UnsupportedCountryException extends RuntimeException {

    public UnsupportedCountryException(String countryCode, String supportedCodes) {
        super("Unsupported country '" + countryCode + "'. Supported countries are " + supportedCodes + ".");
    }
}
