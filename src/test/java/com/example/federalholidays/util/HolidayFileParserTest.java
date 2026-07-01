package com.example.federalholidays.util;

import com.example.federalholidays.dto.HolidayImportRow;
import com.example.federalholidays.exception.HolidayImportException;
import com.example.federalholidays.exception.UnsupportedHolidayFileTypeException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayFileParserTest {

    private final HolidayFileParser parser = new HolidayFileParser(JsonMapper.builder().build());

    @Test
    void parsesCsvHolidayRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.csv",
                "text/csv",
                """
                        name,date,description
                        Canada Day,2026-07-01,National day
                        Labour Day,2026-09-07,
                        """.getBytes(StandardCharsets.UTF_8)
        );

        List<HolidayImportRow> rows = parser.parse(file);

        assertThat(rows).containsExactly(
                new HolidayImportRow(1, "Canada Day", "2026-07-01", "National day"),
                new HolidayImportRow(2, "Labour Day", "2026-09-07", null)
        );
    }

    @Test
    void parsesJsonHolidayRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.json",
                "application/json",
                """
                        [
                          {"name": "Independence Day", "date": "2026-07-04", "description": "Federal holiday"},
                          {"name": "Thanksgiving Day", "date": "2026-11-26"}
                        ]
                        """.getBytes(StandardCharsets.UTF_8)
        );

        List<HolidayImportRow> rows = parser.parse(file);

        assertThat(rows).containsExactly(
                new HolidayImportRow(1, "Independence Day", "2026-07-04", "Federal holiday"),
                new HolidayImportRow(2, "Thanksgiving Day", "2026-11-26", null)
        );
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.txt",
                "text/plain",
                "not,csv".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(UnsupportedHolidayFileTypeException.class)
                .hasMessage("Unsupported upload file type. Use CSV or JSON.");
    }

    @Test
    void rejectsJsonObjectUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.json",
                "application/json",
                "{\"name\":\"Canada Day\"}".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(HolidayImportException.class)
                .hasMessage("JSON upload must be an array of holiday records.");
    }

    @Test
    void treatsEmptyCsvAsNoRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holidays.csv",
                "text/csv",
                "name,date,description\n".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(parser.parse(file)).isEmpty();
    }
}
