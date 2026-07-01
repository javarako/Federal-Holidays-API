package com.example.federalholidays.holiday;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HolidayController.class)
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayService holidayService;

    @Test
    void listsHolidaysForSupportedCountry() throws Exception {
        when(holidayService.listHolidays(any(), any())).thenReturn(List.of(
                new HolidayResponse(1L, "US", "Independence Day", LocalDate.of(2026, 7, 4), null)
        ));

        mockMvc.perform(get("/api/v1/countries/us/holidays")
                        .param("year", "2026")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryCode").value("US"))
                .andExpect(jsonPath("$[0].name").value("Independence Day"));

        verify(holidayService).listHolidays(eq(com.example.federalholidays.domain.CountryCode.US), any(HolidayListFilter.class));
    }

    @Test
    void createsHolidayForSupportedCountry() throws Exception {
        when(holidayService.addHoliday(any(), any())).thenReturn(
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
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.countryCode").value("CA"));
    }

    @Test
    void validatesCreatePayload() throws Exception {
        mockMvc.perform(post("/api/v1/countries/ca/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "date": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void updatesHolidayForSupportedCountry() throws Exception {
        when(holidayService.updateHoliday(any(), eq(2L), any())).thenReturn(
                new HolidayResponse(2L, "CA", "Canada Day", LocalDate.of(2026, 7, 1), "Updated")
        );

        mockMvc.perform(put("/api/v1/countries/ca/holidays/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Canada Day",
                                  "date": "2026-07-01",
                                  "description": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void uploadsCsvFile() throws Exception {
        when(holidayService.importHolidays(any(), any())).thenReturn(
                new HolidayImportResponse(2, 1, 1, List.of(new UploadError(2, "Invalid date format.")))
        );

        mockMvc.perform(multipart("/api/v1/countries/ca/holidays/upload")
                        .file("file", "name,date\nCanada Day,2026-07-01\n".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.successfulRecords").value(1))
                .andExpect(jsonPath("$.failedRecords").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    @Test
    void rejectsUnsupportedCountry() throws Exception {
        mockMvc.perform(get("/api/v1/countries/mx/holidays"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Unsupported country 'mx'. Supported countries are CA, US."));
    }

    @Test
    void rejectsUnsupportedUploadFileType() throws Exception {
        when(holidayService.importHolidays(any(), any()))
                .thenThrow(new UnsupportedHolidayFileTypeException("Unsupported upload file type. Use CSV or JSON."));

        mockMvc.perform(multipart("/api/v1/countries/ca/holidays/upload")
                        .file("file", "hello".getBytes()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }
}
