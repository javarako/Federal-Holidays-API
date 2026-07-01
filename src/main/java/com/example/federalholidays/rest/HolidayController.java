package com.example.federalholidays.rest;

import com.example.federalholidays.entity.enumeration.CountryCode;
import com.example.federalholidays.dto.HolidayImportResponse;
import com.example.federalholidays.dto.HolidayRequest;
import com.example.federalholidays.dto.HolidayResponse;
import com.example.federalholidays.exception.ApiError;
import com.example.federalholidays.filter.HolidayListFilter;
import com.example.federalholidays.service.HolidayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/countries/{countryCode}/holidays")
@Tag(name = "Federal Holidays", description = "Add, update, list, and upload federal holidays by country.")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    @Operation(
            summary = "List federal holidays by country",
            description = "Returns federal holidays for a supported country, ordered by date and name. Optional filters can restrict results by year or date range.",
            parameters = {
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Supported country code. Current values are CA and US.",
                            example = "CA"
                    ),
                    @Parameter(
                            name = "year",
                            in = ParameterIn.QUERY,
                            description = "Optional four digit holiday year filter",
                            example = "2026"
                    ),
                    @Parameter(
                            name = "fromDate",
                            in = ParameterIn.QUERY,
                            description = "Optional inclusive lower date filter using yyyy-MM-dd",
                            example = "2026-01-01"
                    ),
                    @Parameter(
                            name = "toDate",
                            in = ParameterIn.QUERY,
                            description = "Optional inclusive upper date filter using yyyy-MM-dd",
                            example = "2026-12-31"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Holidays returned successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid query parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "404", description = "Country not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    public List<HolidayResponse> listHolidays(
            @PathVariable String countryCode,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        return holidayService.listHolidays(
                CountryCode.fromPath(countryCode),
                HolidayListFilter.from(year, fromDate, toDate)
        );
    }

    @PostMapping
    @Operation(
            summary = "Add a federal holiday",
            description = "Creates a federal holiday for a supported country.",
            parameters = @Parameter(
                    name = "countryCode",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "Supported country code. Current values are CA and US.",
                    example = "CA"
            ),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Holiday details to create",
                    content = @Content(schema = @Schema(implementation = HolidayRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Holiday created successfully", content = @Content(schema = @Schema(implementation = HolidayResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "404", description = "Country not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "409", description = "Duplicate holiday", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    public ResponseEntity<HolidayResponse> addHoliday(
            @PathVariable String countryCode,
            @Valid @RequestBody HolidayRequest request
    ) {
        HolidayResponse response = holidayService.addHoliday(CountryCode.fromPath(countryCode), request);
        URI location = URI.create("/api/v1/countries/" + response.countryCode().toLowerCase() + "/holidays/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a federal holiday",
            description = "Updates an existing federal holiday for the country in the path.",
            parameters = {
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Supported country code. Current values are CA and US.",
                            example = "CA"
                    ),
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Holiday identifier",
                            example = "1"
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Holiday details to update",
                    content = @Content(schema = @Schema(implementation = HolidayRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Holiday updated successfully", content = @Content(schema = @Schema(implementation = HolidayResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "404", description = "Holiday or country not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "409", description = "Duplicate holiday", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    public HolidayResponse updateHoliday(
            @PathVariable String countryCode,
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request
    ) {
        return holidayService.updateHoliday(CountryCode.fromPath(countryCode), id, request);
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Upload federal holidays from CSV or JSON",
            description = "Uploads a CSV or JSON file containing holiday records for the country in the path. Valid records are saved and invalid rows are returned in the summary.",
            parameters = @Parameter(
                    name = "countryCode",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "Supported country code. Current values are CA and US.",
                    example = "CA"
            ),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Multipart form upload with a CSV or JSON file part named file",
                    content = @Content(mediaType = "multipart/form-data")
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Upload processed successfully", content = @Content(schema = @Schema(implementation = HolidayImportResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid upload data", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "404", description = "Country not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "415", description = "Unsupported upload file type", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    public HolidayImportResponse importHolidays(
            @PathVariable String countryCode,
            @Parameter(description = "CSV or JSON file containing holiday records", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return holidayService.importHolidays(CountryCode.fromPath(countryCode), file);
    }
}
