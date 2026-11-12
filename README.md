# Discover Vancouver

**In development** — a location-aware Android application and Spring Boot API
for discovering places around Vancouver.

## Implemented
- Spring Boot API controllers for places, recommendations, weather and health checks.
- MongoDB-backed place model, repository and seed-data support.
- Recommendation parameters for location, category, indoor preference, walking time, budget and atmosphere.
- Distance and recommendation service tests.
- A native Android discovery client with fresh location, persisted preferences,
  favourites, recommendation details, and walking directions.

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
GET http://localhost:8080/api/recommendations?lat=49.2827&lng=-123.1207&maxWalkingMinutes=20
```

## Roadmap
Expand and validate the place dataset, introduce geospatial database queries,
improve recommendation evaluation, and deploy a production API.

**Current scope:** functional local Android MVP connected to the recommendation
backend; production hosting and a real-world place-data pipeline remain future
work.
