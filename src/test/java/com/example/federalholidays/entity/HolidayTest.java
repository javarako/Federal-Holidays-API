package com.example.federalholidays.entity;

import com.example.federalholidays.dto.HolidayRequest;
import com.example.federalholidays.entity.enumeration.CountryCode;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayTest {

    @Test
    void createsHolidayWithExpectedFields() {
        Holiday holiday = new Holiday(CountryCode.US, "Independence Day", LocalDate.of(2026, 7, 4), "Federal holiday");

        assertThat(holiday.getCountryCode()).isEqualTo(CountryCode.US);
        assertThat(holiday.getName()).isEqualTo("Independence Day");
        assertThat(holiday.getHolidayDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(holiday.getDescription()).isEqualTo("Federal holiday");
    }

    @Test
    void updatesMutableFieldsFromRequest() {
        Holiday holiday = new Holiday(CountryCode.CA, "Old Name", LocalDate.of(2026, 1, 1), null);

        holiday.updateFrom(new HolidayRequest("Canada Day", LocalDate.of(2026, 7, 1), "National day"));

        assertThat(holiday.getName()).isEqualTo("Canada Day");
        assertThat(holiday.getHolidayDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(holiday.getDescription()).isEqualTo("National day");
    }
}
