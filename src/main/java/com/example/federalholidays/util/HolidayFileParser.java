package com.example.federalholidays.util;

import com.example.federalholidays.dto.HolidayImportRow;
import com.example.federalholidays.exception.HolidayImportException;
import com.example.federalholidays.exception.UnsupportedHolidayFileTypeException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class HolidayFileParser {

    private final JsonMapper jsonMapper;

    public HolidayFileParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<HolidayImportRow> parse(MultipartFile file) {
        String filename = safeFilename(file);
        if (filename.endsWith(".json") || isJson(file)) {
            return parseJson(file);
        }
        if (filename.endsWith(".csv") || isCsv(file)) {
            return parseCsv(file);
        }
        throw new UnsupportedHolidayFileTypeException("Unsupported upload file type. Use CSV or JSON.");
    }

    private List<HolidayImportRow> parseCsv(MultipartFile file) {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = csvFormat.parse(reader)) {
            List<HolidayImportRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new HolidayImportRow(
                        Math.toIntExact(record.getRecordNumber()),
                        value(record, "name"),
                        value(record, "date"),
                        value(record, "description")
                ));
            }
            return rows;
        } catch (IOException ex) {
            throw new HolidayImportException("Could not read uploaded CSV file.", ex);
        } catch (IllegalArgumentException ex) {
            throw new HolidayImportException("Could not parse uploaded CSV file.", ex);
        }
    }

    private List<HolidayImportRow> parseJson(MultipartFile file) {
        try {
            JsonNode root = jsonMapper.readTree(file.getInputStream());
            if (!root.isArray()) {
                throw new HolidayImportException("JSON upload must be an array of holiday records.");
            }

            List<HolidayImportRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (JsonNode node : root) {
                rows.add(new HolidayImportRow(
                        rowNumber++,
                        text(node, "name"),
                        text(node, "date"),
                        text(node, "description")
                ));
            }
            return rows;
        } catch (IOException ex) {
            throw new HolidayImportException("Could not parse uploaded JSON file.", ex);
        }
    }

    private String value(CSVRecord record, String column) {
        if (!record.isMapped(column) || !record.isSet(column)) {
            return null;
        }
        return clean(record.get(column));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return clean(value.asText());
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isCsv(MultipartFile file) {
        String contentType = file.getContentType();
        return "text/csv".equalsIgnoreCase(contentType) || "application/csv".equalsIgnoreCase(contentType);
    }

    private boolean isJson(MultipartFile file) {
        return "application/json".equalsIgnoreCase(file.getContentType());
    }

    private String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename == null ? "" : filename.toLowerCase();
    }
}
