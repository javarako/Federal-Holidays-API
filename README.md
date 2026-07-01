# Federal Holidays API

REST API for adding, updating, listing, and bulk uploading federal holidays by country.

Supported countries are currently:

- `CA` - Canada
- `US` - United States

The country handling is isolated in `CountryCode`, so adding a country is a small domain change instead of a controller rewrite.

## Run Locally

Start PostgreSQL and the API:

```bash
docker compose up --build
```

The API runs at `http://localhost:8080`.

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

The generated OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`.

A Postman collection is available at [postman/Federal-Holidays-API.postman_collection.json](postman/Federal-Holidays-API.postman_collection.json).

## Endpoints

### List holidays

```bash
curl http://localhost:8080/api/v1/countries/ca/holidays
```

Optional filters:

```bash
curl "http://localhost:8080/api/v1/countries/ca/holidays?year=2026"
curl "http://localhost:8080/api/v1/countries/ca/holidays?fromDate=2026-01-01&toDate=2026-12-31"
```

### Add a holiday

```bash
curl -X POST http://localhost:8080/api/v1/countries/ca/holidays \
  -H "Content-Type: application/json" \
  -d '{"name":"Canada Day","date":"2026-07-01","description":"National day"}'
```

### Update a holiday

```bash
curl -X PUT http://localhost:8080/api/v1/countries/ca/holidays/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Canada Day","date":"2026-07-01","description":"Updated description"}'
```

### Upload holidays from CSV or JSON

CSV headers must include `name` and `date`; `description` is optional.

```csv
name,date,description
Canada Day,2026-07-01,National day
Labour Day,2026-09-07,
```

```bash
curl -X POST http://localhost:8080/api/v1/countries/ca/holidays/upload \
  -F "file=@sample-holidays.csv"
```

JSON uploads use an array of holiday records:

```json
[
  {
    "name": "Canada Day",
    "date": "2026-07-01",
    "description": "National day"
  }
]
```

Uploads return a row-level summary:

```json
{
  "totalRecords": 10,
  "successfulRecords": 9,
  "failedRecords": 1,
  "errors": [
    {
      "row": 4,
      "message": "Invalid date format."
    }
  ]
}
```

## Development

Run tests:

```bash
mvn test
```

Run tests with the 70% coverage gate:

```bash
mvn verify
```

Coverage details are documented in [docs/test-coverage.md](docs/test-coverage.md).
