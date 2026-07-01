package com.example.federalholidays.exception;

public class UnsupportedHolidayFileTypeException extends RuntimeException {

    public UnsupportedHolidayFileTypeException(String message) {
        super(message);
    }
}
