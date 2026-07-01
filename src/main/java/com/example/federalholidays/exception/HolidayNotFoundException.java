package com.example.federalholidays.exception;

import com.example.federalholidays.entity.enumeration.CountryCode;

public class HolidayNotFoundException extends RuntimeException {

    public HolidayNotFoundException(Long id, CountryCode countryCode) {
        super("Holiday " + id + " was not found for country " + countryCode.name() + ".");
    }
}
