INSTALL httpfs;
INSTALL iceberg;
LOAD httpfs;
LOAD iceberg;

CREATE OR REPLACE SECRET iceberg_secret (
    TYPE ICEBERG,
    TOKEN getenv('FSQ_PLACES_ACCESS_TOKEN')
);

ATTACH 'places' AS places (
    TYPE iceberg,
    SECRET iceberg_secret,
    URI 'https://catalog.h3-hub.foursquare.com/iceberg'
);

COPY (
    SELECT
        fsq_place_id,
        name,
        latitude,
        longitude,
        address,
        locality,
        region,
        postcode,
        country,
        date_closed,
        date_refreshed,
        fsq_category_labels,
        unresolved_flags
    FROM places.datasets.places_os
    WHERE country = 'CA'
      AND latitude BETWEEN 49.0 AND 49.5
      AND longitude BETWEEN -123.5 AND -122.5
      AND date_closed IS NULL
      AND try_cast(date_refreshed AS DATE)
          >= current_date - INTERVAL '2 years'
      AND len(fsq_category_labels) > 0
      AND NOT list_has_any(
          coalesce(unresolved_flags, []),
          [
              'closed',
              'duplicate',
              'delete',
              'privatevenue',
              'inappropriate',
              'doesnt_exist'
          ]
      )
      AND regexp_matches(
          lower(cast(fsq_category_labels AS VARCHAR)),
          'coffee|cafe|tea room|bakery|restaurant|food|diner|park|garden|beach|trail|museum|gallery|theater|theatre|arts|library|study|bar|pub|nightclub|music venue|shop|store|mall|market|gym|recreation|sports|pool|attraction|landmark|historic|tourist|monument|zoo|aquarium|scenic|viewpoint'
      )
) TO 'data/snapshots/metro-vancouver.ndjson' (
    FORMAT JSON,
    ARRAY false
);

SELECT count(*) AS exported_places
FROM read_ndjson_auto('data/snapshots/metro-vancouver.ndjson');
