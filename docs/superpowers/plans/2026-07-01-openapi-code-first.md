# OpenAPI Code-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add code-first OpenAPI 3 documentation to the Spring Boot Federal Holidays API and document Swagger UI access.

**Architecture:** Add Springdoc WebMVC UI to generate `/v3/api-docs` and Swagger UI from runtime controller/model metadata. Annotate the controller, DTOs, and error responses with OAS 3 annotations so the generated specification is useful to reviewers.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Web, Spring Validation, Springdoc OpenAPI, Maven, MockMvc.

---

### Task 1: Add OpenAPI Dependency And Metadata

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/example/federalholidays/config/OpenApiConfig.java`
- Test: `src/test/java/com/example/federalholidays/openapi/OpenApiDocumentationTest.java`

- [ ] **Step 1: Write a failing MockMvc test**

Create `OpenApiDocumentationTest` that calls `/v3/api-docs` and expects the Federal Holidays API title and `/api/v1/countries/{countryCode}/holidays` path.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=OpenApiDocumentationTest`
Expected: FAIL because Springdoc is not installed and `/v3/api-docs` is unavailable.

- [ ] **Step 3: Add Springdoc and OpenAPI metadata**

Add `org.springdoc:springdoc-openapi-starter-webmvc-ui` to Maven and configure `OpenAPI` with API title, version, and description.

- [ ] **Step 4: Run the OpenAPI test to verify it passes**

Run: `mvn test -Dtest=OpenApiDocumentationTest`
Expected: PASS.

### Task 2: Annotate API Surface And Models

**Files:**
- Modify: `src/main/java/com/example/federalholidays/holiday/HolidayController.java`
- Modify: `src/main/java/com/example/federalholidays/holiday/HolidayRequest.java`
- Modify: `src/main/java/com/example/federalholidays/holiday/HolidayResponse.java`
- Modify: `src/main/java/com/example/federalholidays/holiday/HolidayImportResponse.java`
- Modify: `src/main/java/com/example/federalholidays/web/ApiError.java`
- Modify: `src/test/java/com/example/federalholidays/holiday/HolidayControllerTest.java`

- [ ] **Step 1: Update controller tests for `/api/v1`**

Change MockMvc URLs from `/api/countries/...` to `/api/v1/countries/...`.

- [ ] **Step 2: Run controller tests to verify the path mismatch fails**

Run: `mvn test -Dtest=HolidayControllerTest`
Expected: FAIL because the controller still maps `/api/countries/...`.

- [ ] **Step 3: Add OAS annotations and versioned API path**

Annotate controller operations, parameters, request bodies, response codes, DTO schemas, and error schema. Change the controller base path to `/api/v1/countries/{countryCode}/holidays`.

- [ ] **Step 4: Run controller tests to verify they pass**

Run: `mvn test -Dtest=HolidayControllerTest`
Expected: PASS.

### Task 3: Update Reviewer Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add Swagger and OpenAPI links**

Document Swagger UI at `http://localhost:8080/swagger-ui/index.html` and OpenAPI JSON at `http://localhost:8080/v3/api-docs`.

- [ ] **Step 2: Run the full verification**

Run: `mvn test`
Expected: PASS with all tests green.
