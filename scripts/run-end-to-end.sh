#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_PORT="${E2E_API_PORT:-18080}"
DATABASE="discover_vancouver_e2e"
RESPONSE_FILE="$(mktemp "${TMPDIR:-/tmp}/discover-vancouver-e2e.XXXXXX.json")"
LOG_FILE="$(mktemp "${TMPDIR:-/tmp}/discover-vancouver-e2e.XXXXXX.log")"
BACKEND_PID=""

cleanup() {
  if [[ -n "${BACKEND_PID}" ]]; then
    kill "${BACKEND_PID}" 2>/dev/null || true
    wait "${BACKEND_PID}" 2>/dev/null || true
  fi
  docker exec discover-vancouver-mongo mongosh \
    "mongodb://localhost:27017/${DATABASE}" \
    --quiet --eval 'db.dropDatabase()' >/dev/null 2>&1 || true
  rm -f "${RESPONSE_FILE}" "${LOG_FILE}"
}
trap cleanup EXIT

cd "${PROJECT_DIR}"
docker compose up -d mongo
docker exec discover-vancouver-mongo mongosh \
  "mongodb://localhost:27017/${DATABASE}" \
  --quiet --eval 'db.dropDatabase()' >/dev/null

mvn --batch-mode package -DskipTests
SPRING_PROFILES_ACTIVE=test \
MONGODB_URI="mongodb://localhost:27017/${DATABASE}" \
SERVER_PORT="${API_PORT}" \
java -jar target/backend-0.0.1-SNAPSHOT.jar >"${LOG_FILE}" 2>&1 &
BACKEND_PID=$!

for _ in {1..60}; do
  if curl --fail --silent \
    "http://localhost:${API_PORT}/actuator/health/readiness" >/dev/null; then
    break
  fi
  if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
    echo "Backend stopped before becoming ready"
    tail -n 80 "${LOG_FILE}"
    exit 1
  fi
  sleep 1
done

curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data-binary @data/e2e/near-place.json \
  "http://localhost:${API_PORT}/api/v1/places" >/dev/null
curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data-binary @data/e2e/far-place.json \
  "http://localhost:${API_PORT}/api/v1/places" >/dev/null

curl --fail --silent --show-error \
  "http://localhost:${API_PORT}/api/v1/recommendations?lat=49.2827&lng=-123.1207&category=contract-test&maxWalkingMinutes=120&limit=10" \
  -o "${RESPONSE_FILE}"

python3 -c '
import json, sys
results = json.load(open(sys.argv[1], encoding="utf-8"))
fixtures = [item for item in results if item["category"] == "contract-test"]
fixture_names = [item["name"] for item in fixtures]
assert fixture_names == [
    "Near Contract Cafe", "Far Contract Cafe"
], f"unexpected fixture order: {fixture_names}; all results: {results}"
assert any("contract-test" in reason for reason in fixtures[0]["reasons"])
assert any("walking range" in reason for reason in fixtures[0]["reasons"])
' "${RESPONSE_FILE}"

JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}" \
  android-app/gradlew \
  -p android-app \
  -De2e.response.file="${RESPONSE_FILE}" \
  testDebugUnitTest \
  --tests '*RecommendationJsonParserTest.parsesEndToEndResponseWhenProvided'

echo "End-to-end recommendation contract passed."
