package com.example.federalholidays.holiday;

import com.example.federalholidays.domain.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByCountryCodeOrderByHolidayDateAscNameAsc(CountryCode countryCode);

    List<Holiday> findByCountryCodeAndHolidayDateBetweenOrderByHolidayDateAscNameAsc(
            CountryCode countryCode,
            LocalDate fromDate,
            LocalDate toDate
    );

    Optional<Holiday> findByIdAndCountryCode(Long id, CountryCode countryCode);

    boolean existsByCountryCodeAndHolidayDateAndName(CountryCode countryCode, LocalDate holidayDate, String name);
}
