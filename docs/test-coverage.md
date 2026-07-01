# Test Coverage

The project enforces automated test coverage with JaCoCo.

## Coverage Requirement

The minimum required coverage is 70% instruction coverage across the application bundle.

The Maven build fails during `verify` if coverage falls below this threshold.

The latest local verification reported 88.24% instruction coverage:

```text
missed=136 covered=1020 coverage=88.24%
```

## How To Run

Run the full verification command:

```bash
mvn verify
```

This command runs the test suite, generates the JaCoCo report, and checks the 70% coverage gate.

## Coverage Report

After `mvn verify`, open the HTML report at:

```text
target/site/jacoco/index.html
```

The XML and CSV coverage reports are also generated in:

```text
target/site/jacoco/
```

## Reviewer Notes

Use `mvn verify` rather than only `mvn test` when checking assignment quality gates. `mvn test` runs the automated tests, but `mvn verify` additionally generates and enforces the coverage report.
