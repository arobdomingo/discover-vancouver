# Free place-source pipeline

The production-oriented catalog combines two complementary free sources:

- **Foursquare Open Places** supplies broad points-of-interest coverage such as
  cafes, restaurants, shops, nightlife, recreation, and attractions.
- **City of Vancouver Open Data** supplies authoritative civic places and
  programs: parks, cultural spaces, libraries, community centres, food
  vendors, and free or low-cost food programs. Park facilities enrich park
  tags.

Both sources are disabled by default. This keeps tests deterministic and avoids
network access or database changes merely from starting the application.

## Architecture

```text
Foursquare Portal export (JSON/NDJSON) ─┐
                                        ├─ source adapters ─ validation
Vancouver Open Data API ────────────────┘         │
                                                  ▼
                                      source-owned MongoDB records
                                                  │
                                    75 m conservative matching
                                                  │
                                                  ▼
                                      shared canonicalPlaceId
                                                  │
                                  filter stale/closed + choose best copy
                                                  │
                                                  ▼
                                       recommendation scoring/API
```

Source records are never destructively merged. Each retains its stable source
identity and provenance; `canonicalPlaceId` merely groups high-confidence
duplicates. This makes an incorrect match repairable and permits source-level
auditing.

## Phase 1: provenance and unknown values

Every imported place can record:

- `source`, `sourceId`, and `sourceUrl`;
- first import, provider refresh, and most recent observation timestamps;
- operating status, stale state, and a quality grade;
- a per-field source map;
- whether price and indoor/outdoor values are actually known.

An empty opening-hours map means **unknown**, not closed. Likewise, a numeric
price default is not treated as a real budget fact when `priceKnown=false`.
Recommendation scoring only awards indoor or price matches when the relevant
fact is known. The API exposes `priceKnown`, `indoorKnown`, `hoursKnown`, and
`source`, and Android labels unknown hours as unavailable.

## Phase 2: Foursquare Open Places

Foursquare now delivers Open Places through its Places Portal and an
Iceberg-based catalog. Create a free Portal account, generate an access token,
and use the Portal's DuckDB (or another Iceberg client) connection snippet to
export only the required regional columns. The application intentionally does
not store a Portal token or download the global catalog itself.

The importer accepts either a JSON array or one JSON object per line. Export at
least these fields:

```text
fsq_place_id, name, latitude, longitude, address, locality, region, postcode,
date_closed, date_refreshed, unresolved_flags, fsq_category_labels
```

The schema also supports the category hierarchy fields
`level1_category_name` through `level6_category_name`. The adapter:

1. restricts records to the configured Metro Vancouver bounding box;
2. excludes records with `date_closed` or unresolved flags indicating a
   suspected closure, duplicate, deletion, private venue, inappropriate entry,
   or nonexistent place;
3. retains records refreshed within two years whose category maps to an MVP
   discovery category, avoiding tens of thousands of unrelated offices and
   services that would otherwise become generic “attractions”;
4. maps the source taxonomy into the app's smaller category/tag vocabulary;
5. treats price as unknown and only claims indoor/outdoor when the category
   mapping supports it;
6. grades freshness from `date_refreshed`.

After placing `FSQ_PLACES_ACCESS_TOKEN` in the ignored `.env`, the repository's
DuckDB script performs the bounded export without embedding the token:

```bash
set -a
source .env
set +a
duckdb -no-agent < scripts/export-foursquare-places.sql
```

The resulting `data/snapshots/metro-vancouver.ndjson` directory is ignored by
Git because the downloaded catalog can be regenerated and must retain its
licensing notices outside the source history.

Run a local snapshot plus City sources:

```bash
PLACE_SOURCES_IMPORT_ON_STARTUP=true \
FOURSQUARE_OPEN_PLACES_ENABLED=true \
FOURSQUARE_OPEN_PLACES_FILE=/absolute/path/metro-vancouver.ndjson \
VANCOUVER_OPEN_DATA_ENABLED=true \
mvn spring-boot:run
```

The default geographic filter is latitude `49.0..49.5` and longitude
`-123.5..-122.5`. Override `PLACES_MIN_LATITUDE`, `PLACES_MAX_LATITUDE`,
`PLACES_MIN_LONGITUDE`, and `PLACES_MAX_LONGITUDE` if required.

## Phase 3: Vancouver Open Data

The API client reads each dataset page from the Opendatasoft 2.1 catalog,
applies connect/read timeouts, and tolerates common field-name variations. It
accepts coordinates represented as separate latitude/longitude values, a geo
point, GeoJSON, or the City's map-destination string. Records without a name or
usable Metro Vancouver coordinate are skipped.

Enable it with:

```bash
PLACE_SOURCES_IMPORT_ON_STARTUP=true \
VANCOUVER_OPEN_DATA_ENABLED=true \
mvn spring-boot:run
```

Useful overrides are `VANCOUVER_OPEN_DATA_BASE_URL`,
`VANCOUVER_OPEN_DATA_PAGE_SIZE`, `VANCOUVER_OPEN_DATA_CONNECT_TIMEOUT`, and
`VANCOUVER_OPEN_DATA_READ_TIMEOUT`.

## Phases 4–5: identity and recommendation behavior

`(source, sourceId)` has a unique sparse MongoDB index, so re-importing updates
instead of duplicating a provider record. Cross-source matching then looks only
within 75 metres and uses normalized names and addresses. The app selects one
representative per canonical group, preferring verified/high-quality and more
complete records.

Places marked stale or temporarily/permanently closed do not reach the scoring
stage. Unknown fields remain eligible but do not receive a positive match
bonus. This avoids the two most damaging assumptions: presenting missing hours
as “closed” and presenting a default price as verified.

## Phase 6: refreshes and failures

The scheduler defaults to a seven-day fixed delay and a five-minute initial
delay. Set `PLACE_SOURCES_REFRESH_DELAY` and `PLACE_SOURCES_INITIAL_DELAY` using
ISO-8601 durations such as `PT24H`.

Each completed source snapshot updates seen records and marks missing records
stale. It does not delete them. A failed source download logs the exception and
leaves the last successful catalog intact. Import reports log created, updated,
rejected, stale, and duplicate-linked counts.

For Foursquare, replace the configured export file before the scheduled job or
restart with startup import enabled. A later deployment can automate the Portal
query as a separate data job without coupling credentials to the web service.

## Phase 7: quality controls

The automated tests cover source parsing, category/coordinate mapping,
validation, idempotent updates, snapshot staleness, conservative canonical
matching, recommendation behavior, repositories, and the full API. Before a
release, run:

```bash
mvn test

cd android-app
./gradlew test
```

Operationally, review refresh reports for sudden changes in rejects, stale
counts, or volume. Manually sample canonical groups after changing matching
rules. Never silently broaden the matcher merely to reduce duplicate counts.

## Licensing and attribution

Foursquare Open Places is provided under Apache License 2.0 and requires its
NOTICE attribution to be preserved when the data is used, modified, or
distributed. City data is provided under the Open Government Licence –
Vancouver and requires source acknowledgement. Required notices are recorded
in [`THIRD_PARTY_DATA.md`](../THIRD_PARTY_DATA.md). Do not commit downloaded
place snapshots to this repository unless their licence obligations and the
privacy/security implications have been reviewed.

References:

- [Access FSQ Open Places](https://docs.foursquare.com/data-products/docs/access-fsq-os-places)
- [Foursquare OS Places schema](https://docs.foursquare.com/data-products/docs/places-os-data-schema)
- [Foursquare Open Places licensing](https://docs.foursquare.com/data-products/docs/fsq-places-open-source)
- [City of Vancouver Open Data licence](https://opendata.vancouver.ca/pages/licence/)
