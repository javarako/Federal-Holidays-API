package com.example.federalholidays.holiday;

public class UnsupportedHolidayFileTypeException extends RuntimeException {

    public UnsupportedHolidayFileTypeException(String message) {
        super(message);
    }
}
