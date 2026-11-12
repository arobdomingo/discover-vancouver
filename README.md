# Discover Vancouver

**In development** — a location-aware Android application and Spring Boot API
for discovering places around Vancouver.

## Implemented
- Spring Boot API controllers for places, recommendations, weather and health checks.
- MongoDB-backed place model, seed-data migration, and GeoJSON nearby-place
  queries using a `2dsphere` index.
- Recommendation parameters for location, category, indoor preference, walking time, budget and atmosphere.
- Controlled CSV/JSON place imports with validation, normalization,
  provenance, and source-based updates.
- Anonymous recommendation feedback events and aggregate engagement counts.
- Distance and recommendation service tests.
- A native Android discovery client with fresh location, persisted preferences,
  favourites, recommendation details, and walking directions.
- Versioned and rate-limited API endpoints with request IDs, pagination,
  consistent errors, OpenAPI documentation, health probes, Prometheus metrics,
  structured production logs, and weather fallback behavior.
- Automated backend, Android and end-to-end contract tests with CI artifacts,
  dependency scanning, container builds, and approval-gated releases.

## Stack
Java, Spring Boot, Maven, MongoDB, Docker Compose, Android, and Gradle.

## Structure
`src/main/java/com/discovervancouver/` contains `controller/`, `service/`, `model/`, `repository/`, `dto/` and `config/`. Tests are in `src/test/`.
The native client and its tests are in `android-app/`.

## Run locally
Start MongoDB at `mongodb://localhost:27017/discover_vancouver` (the default configuration), then run:

```bash
mvn spring-boot:run
mvn test
```

Example request:

```http
GET http://localhost:8080/api/v1/recommendations?lat=49.2827&lng=-123.1207&maxWalkingMinutes=20
```

Import the example dataset:

```bash
PLACE_IMPORT_FILE="$PWD/data/examples/places.csv" \
PLACE_IMPORT_SOURCE=example-data \
mvn spring-boot:run
```

See [the place import guide](docs/place-import.md) for the CSV/JSON schema and
deduplication rules. Anonymous interaction totals are available from
`GET /api/v1/feedback/summary`.

Production configuration, observability, backups and container deployment are
documented in [the operations guide](docs/production-operations.md). Test
layers, the executable end-to-end check and release automation are documented
in [the testing guide](docs/testing.md).

## Roadmap
Provision managed MongoDB and HTTPS hosting, then begin a closed beta with
production monitoring and tester interviews.

**Current scope:** functional local Android MVP connected to the recommendation
backend with indexed geospatial queries, controlled data ingestion,
recommendation calibration scenarios, an anonymous feedback loop, production
operational controls, and automated verification.
