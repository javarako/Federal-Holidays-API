package com.example.federalholidays.filter;

import com.example.federalholidays.exception.InvalidHolidayQueryException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayListFilterTest {

    @Test
    void createsEmptyFilterWhenNoQueryParametersAreProvided() {
        HolidayListFilter filter = HolidayListFilter.from(null, null, null);

        assertThat(filter.hasFilters()).isFalse();
        assertThat(filter.effectiveFromDate()).isEqualTo(LocalDate.of(1, 1, 1));
        assertThat(filter.effectiveToDate()).isEqualTo(LocalDate.of(9999, 12, 31));
    }

    @Test
    void combinesYearAndDateRangeFilters() {
        HolidayListFilter filter = HolidayListFilter.from("2026", "2026-06-01", "2026-12-01");

        assertThat(filter.hasFilters()).isTrue();
        assertThat(filter.effectiveFromDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(filter.effectiveToDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(filter.isEmptyIntersection()).isFalse();
    }

    @Test
    void detectsEmptyIntersectionBetweenYearAndDateRange() {
        HolidayListFilter filter = HolidayListFilter.from("2026", "2027-01-01", "2027-12-31");

        assertThat(filter.isEmptyIntersection()).isTrue();
    }

    @Test
    void rejectsInvalidYearFormat() {
        assertThatThrownBy(() -> HolidayListFilter.from("26", null, null))
                .isInstanceOf(InvalidHolidayQueryException.class)
                .hasMessage("year must use a four digit format.");
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThatThrownBy(() -> HolidayListFilter.from(null, "2026-12-31", "2026-01-01"))
                .isInstanceOf(InvalidHolidayQueryException.class)
                .hasMessage("fromDate must be before or equal to toDate.");
    }
}
