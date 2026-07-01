package com.example.federalholidays.holiday;

import com.example.federalholidays.domain.CountryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "holidays",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_holiday_country_date_name",
                columnNames = {"country_code", "holiday_date", "name"}
        )
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_code", nullable = false, length = 2)
    private CountryCode countryCode;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Holiday() {
    }

    public Holiday(CountryCode countryCode, String name, LocalDate holidayDate, String description) {
        this.countryCode = countryCode;
        this.name = name;
        this.holidayDate = holidayDate;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }

    public String getName() {
        return name;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getDescription() {
        return description;
    }

    public void updateFrom(HolidayRequest request) {
        this.name = request.name();
        this.holidayDate = request.date();
        this.description = request.description();
    }
}
