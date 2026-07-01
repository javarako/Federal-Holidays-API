package com.example.federalholidays.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record HolidayListFilter(Integer year, LocalDate fromDate, LocalDate toDate) {

    private static final LocalDate MIN_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    public static HolidayListFilter empty() {
        return new HolidayListFilter(null, null, null);
    }

    public static HolidayListFilter from(String year, String fromDate, String toDate) {
        Integer parsedYear = parseYear(year);
        LocalDate parsedFromDate = parseDate(fromDate, "fromDate");
        LocalDate parsedToDate = parseDate(toDate, "toDate");

        if (parsedFromDate != null && parsedToDate != null && parsedFromDate.isAfter(parsedToDate)) {
            throw new InvalidHolidayQueryException("fromDate must be before or equal to toDate.");
        }

        return new HolidayListFilter(parsedYear, parsedFromDate, parsedToDate);
    }

    boolean hasFilters() {
        return year != null || fromDate != null || toDate != null;
    }

    LocalDate effectiveFromDate() {
        LocalDate lowerBound = year == null ? MIN_DATE : LocalDate.of(year, 1, 1);
        if (fromDate != null && fromDate.isAfter(lowerBound)) {
            return fromDate;
        }
        return lowerBound;
    }

    LocalDate effectiveToDate() {
        LocalDate upperBound = year == null ? MAX_DATE : LocalDate.of(year, 12, 31);
        if (toDate != null && toDate.isBefore(upperBound)) {
            return toDate;
        }
        return upperBound;
    }

    boolean isEmptyIntersection() {
        return effectiveFromDate().isAfter(effectiveToDate());
    }

    private static Integer parseYear(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches("\\d{4}")) {
            throw new InvalidHolidayQueryException("year must use a four digit format.");
        }
        return Integer.valueOf(value);
    }

    private static LocalDate parseDate(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidHolidayQueryException(parameterName + " must use ISO date format yyyy-MM-dd.");
        }
    }
}
