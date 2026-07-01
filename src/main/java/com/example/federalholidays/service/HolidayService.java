package com.example.federalholidays.service;

import com.example.federalholidays.entity.enumeration.CountryCode;
import com.example.federalholidays.dto.HolidayImportResponse;
import com.example.federalholidays.dto.HolidayImportRow;
import com.example.federalholidays.dto.HolidayRequest;
import com.example.federalholidays.dto.HolidayResponse;
import com.example.federalholidays.dto.UploadError;
import com.example.federalholidays.entity.Holiday;
import com.example.federalholidays.exception.HolidayImportException;
import com.example.federalholidays.exception.HolidayNotFoundException;
import com.example.federalholidays.filter.HolidayListFilter;
import com.example.federalholidays.repo.HolidayRepository;
import com.example.federalholidays.util.HolidayFileParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HolidayService {

    private static final long MAX_UPLOAD_BYTES = 1024 * 1024;

    private final HolidayRepository holidayRepository;
    private final HolidayFileParser holidayFileParser;

    public HolidayService(HolidayRepository holidayRepository, HolidayFileParser holidayFileParser) {
        this.holidayRepository = holidayRepository;
        this.holidayFileParser = holidayFileParser;
    }

    @Transactional
    public HolidayResponse addHoliday(CountryCode countryCode, HolidayRequest request) {
        Holiday holiday = new Holiday(countryCode, request.name(), request.date(), request.description());
        return HolidayResponse.from(holidayRepository.save(holiday));
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> listHolidays(CountryCode countryCode, HolidayListFilter filter) {
        HolidayListFilter effectiveFilter = filter == null ? HolidayListFilter.empty() : filter;
        if (!effectiveFilter.hasFilters()) {
            return holidayRepository.findByCountryCodeOrderByHolidayDateAscNameAsc(countryCode)
                    .stream()
                    .map(HolidayResponse::from)
                    .toList();
        }
        if (effectiveFilter.isEmptyIntersection()) {
            return List.of();
        }
        return holidayRepository.findByCountryCodeAndHolidayDateBetweenOrderByHolidayDateAscNameAsc(
                        countryCode,
                        effectiveFilter.effectiveFromDate(),
                        effectiveFilter.effectiveToDate()
                )
                .stream()
                .map(HolidayResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> listHolidays(CountryCode countryCode) {
        return holidayRepository.findByCountryCodeOrderByHolidayDateAscNameAsc(countryCode)
                .stream()
                .map(HolidayResponse::from)
                .toList();
    }

    @Transactional
    public HolidayResponse updateHoliday(CountryCode countryCode, Long id, HolidayRequest request) {
        Holiday holiday = holidayRepository.findByIdAndCountryCode(id, countryCode)
                .orElseThrow(() -> new HolidayNotFoundException(id, countryCode));
        holiday.updateFrom(request);
        return HolidayResponse.from(holidayRepository.save(holiday));
    }

    @Transactional
    public HolidayImportResponse importHolidays(CountryCode countryCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new HolidayImportException("Upload file is required.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new HolidayImportException("Upload file must not exceed 1 MB.");
        }

        List<HolidayImportRow> rows = holidayFileParser.parse(file);
        List<Holiday> validHolidays = new ArrayList<>();
        List<UploadError> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();

        for (HolidayImportRow row : rows) {
            Holiday holiday = toHoliday(countryCode, row, errors, seenInFile);
            if (holiday != null) {
                validHolidays.add(holiday);
            }
        }

        if (!validHolidays.isEmpty()) {
            holidayRepository.saveAll(validHolidays);
        }

        return new HolidayImportResponse(
                rows.size(),
                validHolidays.size(),
                errors.size(),
                List.copyOf(errors)
        );
    }

    private Holiday toHoliday(
            CountryCode countryCode,
            HolidayImportRow row,
            List<UploadError> errors,
            Set<String> seenInFile
    ) {
        String name = row.name();
        if (name == null || name.isBlank()) {
            errors.add(new UploadError(row.rowNumber(), "name is required."));
            return null;
        }
        if (name.length() > 160) {
            errors.add(new UploadError(row.rowNumber(), "name must not exceed 160 characters."));
            return null;
        }
        if (row.date() == null || row.date().isBlank()) {
            errors.add(new UploadError(row.rowNumber(), "date is required."));
            return null;
        }

        LocalDate holidayDate;
        try {
            holidayDate = LocalDate.parse(row.date());
        } catch (DateTimeParseException ex) {
            errors.add(new UploadError(row.rowNumber(), "Invalid date format."));
            return null;
        }

        if (row.description() != null && row.description().length() > 500) {
            errors.add(new UploadError(row.rowNumber(), "description must not exceed 500 characters."));
            return null;
        }

        String duplicateKey = countryCode + "|" + holidayDate + "|" + name;
        if (!seenInFile.add(duplicateKey) || holidayRepository.existsByCountryCodeAndHolidayDateAndName(countryCode, holidayDate, name)) {
            errors.add(new UploadError(row.rowNumber(), "Duplicate holiday."));
            return null;
        }

        return new Holiday(countryCode, name, holidayDate, row.description());
    }
}
