package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.discovervancouver.config.PlaceSourceProperties;
import com.discovervancouver.model.DataQuality;

import tools.jackson.databind.ObjectMapper;

class FoursquareOpenPlacesFileReaderTest {

    @TempDir
    Path directory;

    @Test
    void mapsOpenPlaceAndFiltersClosedAndOutOfAreaRecords() throws Exception {
        Path file = directory.resolve("places.ndjson");
        Files.writeString(file, """
            {"fsq_place_id":"cafe-1","name":"Harbour Coffee","latitude":49.2827,"longitude":-123.1207,"address":"100 Test St","locality":"Vancouver","region":"BC","fsq_category_labels":["Coffee Shop"],"date_refreshed":"2026-10-01T00:00:00Z"}
            {"fsq_place_id":"closed-1","name":"Closed Cafe","latitude":49.28,"longitude":-123.12,"fsq_category_labels":["Cafe"],"date_closed":"2026-01-01"}
            {"fsq_place_id":"flagged-1","name":"Suspect Cafe","latitude":49.28,"longitude":-123.12,"fsq_category_labels":["Cafe"],"unresolved_flags":["doesnt_exist"]}
            {"fsq_place_id":"far-1","name":"Far Cafe","latitude":50.0,"longitude":-123.12,"fsq_category_labels":["Cafe"]}
            """);
        PlaceSourceProperties properties = new PlaceSourceProperties();
        FoursquareOpenPlacesFileReader reader =
            new FoursquareOpenPlacesFileReader(
                new ObjectMapper(),
                new FoursquareCategoryMapper(),
                properties
            );

        List<PlaceImportRecord> records = reader.read(file);

        assertEquals(1, records.size());
        PlaceImportRecord record = records.get(0);
        assertEquals("cafe", record.category());
        assertEquals("100 Test St, Vancouver, BC", record.address());
        assertTrue(record.tags().contains("coffee-shop"));
        assertFalse(record.priceKnown());
        assertEquals(DataQuality.HIGH, record.dataQuality());
    }
}
