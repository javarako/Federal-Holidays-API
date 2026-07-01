package com.example.federalholidays.rest;

import com.example.federalholidays.dto.HolidayImportResponse;
import com.example.federalholidays.dto.HolidayResponse;
import com.example.federalholidays.dto.UploadError;
import com.example.federalholidays.entity.enumeration.CountryCode;
import com.example.federalholidays.exception.HolidayImportException;
import com.example.federalholidays.exception.UnsupportedHolidayFileTypeException;
import com.example.federalholidays.filter.HolidayListFilter;
import com.example.federalholidays.service.HolidayService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HolidayController.class)
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayService holidayService;

    @Test
    void addsHolidayEndpoint() throws Exception {
        when(holidayService.addHoliday(eq(CountryCode.CA), any())).thenReturn(
                new HolidayResponse(2L, "CA", "Canada Day", LocalDate.of(2026, 7, 1), "National day")
        );

        mockMvc.perform(post("/api/v1/countries/ca/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Canada Day",
                                  "date": "2026-07-01",
                                  "description": "National day"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/countries/ca/holidays/2"))
                .andExpect(jsonPath("$.countryCode").value("CA"))
                .andExpect(jsonPath("$.name").value("Canada Day"));
    }

    @Test
    void updatesHolidayEndpoint() throws Exception {
        when(holidayService.updateHoliday(eq(CountryCode.US), eq(7L), any())).thenReturn(
                new HolidayResponse(7L, "US", "Independence Day", LocalDate.of(2026, 7, 4), "Updated")
        );

        mockMvc.perform(put("/api/v1/countries/us/holidays/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Independence Day",
                                  "date": "2026-07-04",
                                  "description": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Independence Day"));
    }

    @Test
    void listsHolidaysEndpointWithOptionalQueryParameters() throws Exception {
        when(holidayService.listHolidays(eq(CountryCode.CA), any(HolidayListFilter.class))).thenReturn(List.of(
                new HolidayResponse(1L, "CA", "Canada Day", LocalDate.of(2026, 7, 1), null)
        ));

        mockMvc.perform(get("/api/v1/countries/ca/holidays")
                        .param("year", "2026")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryCode").value("CA"))
                .andExpect(jsonPath("$[0].name").value("Canada Day"));

        verify(holidayService).listHolidays(eq(CountryCode.CA), eq(new HolidayListFilter(
                2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )));
    }

    @Test
    void uploadsFileEndpoint() throws Exception {
        when(holidayService.importHolidays(eq(CountryCode.US), any())).thenReturn(
                new HolidayImportResponse(2, 1, 1, List.of(new UploadError(2, "Invalid date format.")))
        );

        mockMvc.perform(multipart("/api/v1/countries/us/holidays/upload")
                        .file("file", "name,date,description\nIndependence Day,2026-07-04,\n".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.successfulRecords").value(1))
                .andExpect(jsonPath("$.failedRecords").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    @Test
    void rejectsUnsupportedCountryCode() throws Exception {
        mockMvc.perform(get("/api/v1/countries/mx/holidays"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unsupported country 'mx'. Supported countries are CA, US."));
    }

    @Test
    void rejectsMissingHolidayName() throws Exception {
        mockMvc.perform(post("/api/v1/countries/ca/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "date": "2026-07-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("name: name is required"));
    }

    @Test
    void rejectsInvalidHolidayDate() throws Exception {
        mockMvc.perform(post("/api/v1/countries/ca/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Canada Day",
                                  "date": "07/01/2026"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request data."));
    }

    @Test
    void returnsConflictForDuplicateHoliday() throws Exception {
        when(holidayService.addHoliday(eq(CountryCode.CA), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/api/v1/countries/ca/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Canada Day",
                                  "date": "2026-07-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Holiday already exists for that country, date, and name."));
    }

    @Test
    void rejectsInvalidUploadFileFormat() throws Exception {
        when(holidayService.importHolidays(eq(CountryCode.CA), any()))
                .thenThrow(new UnsupportedHolidayFileTypeException("Unsupported upload file type. Use CSV or JSON."));

        mockMvc.perform(multipart("/api/v1/countries/ca/holidays/upload")
                        .file("file", "plain text".getBytes()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported upload file type. Use CSV or JSON."));
    }

    @Test
    void rejectsEmptyUploadFile() throws Exception {
        when(holidayService.importHolidays(eq(CountryCode.CA), any()))
                .thenThrow(new HolidayImportException("Upload file is required."));

        mockMvc.perform(multipart("/api/v1/countries/ca/holidays/upload")
                        .file("file", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload file is required."));
    }
}
