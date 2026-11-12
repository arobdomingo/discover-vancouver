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
The stored place retains its source, source ID, and latest import timestamp.

## CSV schema

Required headers are:

```text
sourceId,name,category,description,address,priceLevel,indoor,latitude,longitude,tags
```

Use standard CSV quoting for commas and double quotes. Separate tags with `|`.
CSV does not currently carry opening hours, so imported hours are considered
unknown and the place will not satisfy an `openOnly` request.

## JSON schema

JSON files contain an array of records using the same fields. `tags` is an
array. `openingHours` is optional; when supplied, it maps uppercase weekday
names to arrays of `{ "opensAt": "HH:mm", "closesAt": "HH:mm" }` objects.

The importer validates field lengths, coordinates, prices, and tags. It trims
text, normalizes categories and tags to lowercase, removes repeated tags, and
reports rejected records without saving them. Example files are in
`data/examples/`.
