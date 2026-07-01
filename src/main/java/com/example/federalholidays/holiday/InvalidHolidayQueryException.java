package com.example.federalholidays.holiday;

public class InvalidHolidayQueryException extends RuntimeException {

    public InvalidHolidayQueryException(String message) {
        super(message);
    }
}
