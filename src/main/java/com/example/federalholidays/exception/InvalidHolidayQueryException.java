package com.example.federalholidays.exception;

public class InvalidHolidayQueryException extends RuntimeException {

    public InvalidHolidayQueryException(String message) {
        super(message);
    }
}
