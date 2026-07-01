# Federal Holidays API Design

## Purpose

Build a RESTful API for managing federal holidays by country. The API supports adding, updating, listing, and bulk uploading holidays. For this assignment, supported countries are Canada and the United States, represented as `CA` and `US`.

The current Spring Boot implementation is the intended target for this design.

## Scope

The API manages holiday records with these fields:

- `id`: server-generated numeric identifier.
- `countryCode`: supported country code, currently `CA` or `US`.
- `name`: required holiday name.
- `date`: required holiday date in ISO format, such as `2026-07-01`.
- `description`: optional text description.

Supported endpoints:

```text
GET  /api/v1/countries/{countryCode}/holidays
POST /api/v1/countries/{countryCode}/holidays
PUT  /api/v1/countries/{countryCode}/holidays/{id}
POST /api/v1/countries/{countryCode}/holidays/upload
```

The list endpoint supports optional query parameters:

- `year`: returns holidays whose date falls within that calendar year.
- `fromDate`: returns holidays on or after this ISO date.
- `toDate`: returns holidays on or before this ISO date.

Example:

```text
GET /api/v1/countries/CA/holidays?year=2026
```

If multiple list filters are provided, all filters are applied together. For example, `fromDate=2026-07-01&toDate=2026-12-31` returns holidays in that inclusive date range. If `year` is combined with `fromDate` or `toDate`, the response must satisfy both the year and date-bound filters.

The upload endpoint accepts CSV or JSON files. CSV files use `name` and `date` headers, with optional `description`. JSON files use an array of objects with `name`, `date`, and optional `description`. The country always comes from the URL path, not the uploaded file.

The upload response summarizes processing results:

```json
{
  "totalRecords": 10,
  "successfulRecords": 9,
  "failedRecords": 1,
  "errors": [
    {
      "row": 4,
      "message": "Invalid date format"
    }
  ]
}
```

## Architecture

The service is a conventional Spring Boot Maven application using Spring Web, Spring Validation, Spring Data JPA, and PostgreSQL.

Main components:

- `FederalHolidaysApiApplication`: Spring Boot application entry point.
- `HolidayController`: REST controller for holiday endpoints.
- `HolidayService`: business logic for add, update, list, and upload import operations.
- `HolidayFileParser`: parses supported CSV and JSON upload files into holiday import rows.
- `HolidayRepository`: Spring Data JPA repository for holiday persistence.
- `Holiday`: JPA entity mapped to the `holidays` table.
- `CountryCode`: enum that centralizes supported country handling.
- `GlobalExceptionHandler`: maps domain, validation, import, and persistence errors to JSON API errors.

Country support is intentionally isolated in `CountryCode`. Adding a future country should primarily require adding a new enum value, while the controller and service flow remain unchanged.

## Data Model

The `Holiday` entity stores:

- `id`: primary key.
- `countryCode`: enum stored as a two-letter string.
- `name`: required, maximum 160 characters.
- `holidayDate`: required date column.
- `description`: optional, maximum 500 characters.
- `createdAt`: creation timestamp.
- `updatedAt`: last update timestamp.

The database enforces uniqueness for `country_code`, `holiday_date`, and `name` together. This prevents exact duplicate holiday entries for the same country and date.

## Request Flow

For list requests, the controller parses `{countryCode}`, validates that it is supported, accepts optional `year`, `fromDate`, and `toDate` filters, and asks the service for holidays ordered by date and name.

For add requests, Spring Validation checks the JSON body. The service creates a new `Holiday` for the path country and persists it.

For update requests, the service looks up the holiday by both `id` and country. This prevents updating a holiday through the wrong country path. The service updates `name`, `date`, and `description`, but the country remains unchanged.

For upload requests, the service receives a multipart file, validates the file type and size, parses the file content, validates each holiday record, saves valid records, and returns an upload summary. Each row is parsed into a holiday for the path country. Required row data is `name` and `date`; `description` is optional.

Invalid upload rows are skipped and reported in the `errors` array. Valid rows are still saved when other rows in the same file fail validation.

## Validation And Errors

JSON request validation rules:

- `name` is required and must be at most 160 characters.
- `date` is required.
- `description` is optional and must be at most 500 characters.

List query validation rules:

- `year` must be a four-digit integer when present.
- `fromDate` must use ISO local date format when present.
- `toDate` must use ISO local date format when present.
- `fromDate` must be less than or equal to `toDate` when both are present.

Upload import rules:

- A file must be present and non-empty.
- The file type must be supported. Supported upload types are CSV and JSON, accepted by recognized content type or `.csv`/`.json` filename.
- The file must not exceed 1 MB.
- The record must provide a `name` value.
- The record must provide a `date` value.
- Dates must use ISO local date format.
- `description` is optional.
- Each row must pass the same holiday data rules as JSON create/update requests.

Errors are returned as `ApiError` JSON with:

- `timestamp`
- `status`
- `error`
- `message`
- `details`

Status behavior:

- `200 OK`: successful list, update, or upload request.
- `201 Created`: holiday created successfully.
- `400 Bad Request`: invalid request data, validation failure, invalid list query parameters, missing upload file, malformed upload content, missing required upload values, invalid upload dates, or upload file too large.
- `404 Not Found`: unsupported country code or update target does not exist for the path country.
- `409 Conflict`: duplicate country/date/name holiday violates the database uniqueness rule.
- `415 Unsupported Media Type`: upload file type is not supported.
- `500 Internal Server Error`: unexpected server error.

## Local Runtime

The reviewer run path is Docker-first:

```bash
docker compose up --build
```

Docker Compose starts:

- `postgres`: PostgreSQL 16 with database `federal_holidays`.
- `api`: Spring Boot application listening on port `8080`.

The API container depends on PostgreSQL health checks before startup. Datasource values are passed through environment variables. Hibernate schema update is enabled for this assignment-sized local service.

## Testing

The test suite covers the intended behavior at focused levels:

- `CountryCodeTest`: supported country parsing and unsupported country rejection.
- `HolidayServiceTest`: add, filtered list, update, missing holiday handling, CSV/JSON import, row-level upload validation, duplicate handling, unsupported file type handling, and upload summary counts.
- `HolidayControllerTest`: endpoint status codes, response JSON, request validation, list query parameter handling, unsupported country handling, and multipart upload wiring.

Development verification command:

```bash
mvn test
```

Packaging verification command:

```bash
mvn -q -DskipTests package
```

Docker Compose configuration verification command:

```bash
docker compose config
```
