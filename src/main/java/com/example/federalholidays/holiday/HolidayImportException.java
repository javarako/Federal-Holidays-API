package com.example.federalholidays.holiday;

public class HolidayImportException extends RuntimeException {

    public HolidayImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public HolidayImportException(String message) {
        super(message);
    }
}
