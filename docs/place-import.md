# Place data import

The backend can import controlled CSV or JSON datasets when it starts. Importing
is disabled unless both `PLACE_IMPORT_FILE` and `PLACE_IMPORT_SOURCE` are set.

```bash
PLACE_IMPORT_FILE=/absolute/path/places.csv \
PLACE_IMPORT_SOURCE=vancouver-open-data \
mvn spring-boot:run
```

`PLACE_IMPORT_SOURCE` identifies the dataset. Each record also needs a stable
`sourceId` from that dataset. The pair is used as the identity: importing it
again updates the existing MongoDB document instead of creating a duplicate.
The stored place retains its source, source ID, source URL, import and refresh
timestamps, operating status, quality grade, per-field provenance, and flags
that distinguish a known value from an unknown one.

## CSV schema

Required headers are:

```text
sourceId,name,category,description,address,priceLevel,indoor,latitude,longitude,tags
```

Use standard CSV quoting for commas and double quotes. Separate tags with `|`.
CSV does not currently carry opening hours, so imported hours are considered
unknown. Unknown hours are not treated as closed: an `openOnly` request removes
places known to be closed but keeps places whose hours cannot be established.

## JSON schema

JSON files contain an array of records using the same fields. `tags` is an
array. `openingHours` is optional; when supplied, it maps uppercase weekday
names to arrays of `{ "opensAt": "HH:mm", "closesAt": "HH:mm" }` objects.

The importer validates field lengths, coordinates, prices, and tags. It trims
text, normalizes categories and tags to lowercase, removes repeated tags, and
reports rejected records without saving them. Example files are in
`data/examples/`.

## Snapshot and deduplication behavior

The generic `PLACE_IMPORT_FILE` path is an incremental import. The automated
Foursquare and Vancouver adapters use complete snapshots. A source record not
seen in its next complete snapshot is marked stale rather than deleted, making
the change reversible and preventing it from appearing in recommendations.

Records remain separate source documents for traceability. A canonical place
ID connects records only when they are within 75 metres and either have the
same normalized name, or have the same normalized address plus at least 60%
name-token similarity. This deliberately favors missed duplicates over merging
two real businesses incorrectly. Recommendation queries return the strongest
available representative of each canonical place.
