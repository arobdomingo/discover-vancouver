package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

class PlaceImportFileReaderTest {

    private final PlaceImportFileReader reader =
        new PlaceImportFileReader(new ObjectMapper());

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsJsonImportFile() throws Exception {
        Path file = temporaryDirectory.resolve("places.json");
        Files.writeString(file, """
            [{
              "sourceId": "cafe-1",
              "name": "Example Cafe",
              "category": "cafe",
              "description": "A calm cafe.",
              "address": "123 Example St",
              "priceLevel": 1,
              "indoor": true,
              "latitude": 49.2827,
              "longitude": -123.1207,
              "tags": ["quiet", "wifi"]
            }]
            """);

        List<PlaceImportRecord> records = reader.read(file);

        assertEquals(1, records.size());
        assertEquals("cafe-1", records.get(0).sourceId());
    }

    @Test
    void readsQuotedCsvAndPipeSeparatedTags() throws Exception {
        Path file = temporaryDirectory.resolve("places.csv");
        Files.writeString(file, """
            sourceId,name,category,description,address,priceLevel,indoor,latitude,longitude,tags
            cafe-1,"Example, Cafe",cafe,"Calm, bright cafe",123 Example St,1,true,49.2827,-123.1207,quiet|wifi
            """);

        List<PlaceImportRecord> records = reader.read(file);

        assertEquals("Example, Cafe", records.get(0).name());
        assertEquals(List.of("quiet", "wifi"), records.get(0).tags());
    }
}
