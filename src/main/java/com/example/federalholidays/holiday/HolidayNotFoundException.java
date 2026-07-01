package com.example.federalholidays.holiday;

import com.example.federalholidays.domain.CountryCode;

public class HolidayNotFoundException extends RuntimeException {

    public HolidayNotFoundException(Long id, CountryCode countryCode) {
        super("Holiday " + id + " was not found for country " + countryCode.name() + ".");
    }
}
