package com.discovervancouver.importer;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class FoursquareOpenPlacesImporter {

    public static final String SOURCE = "foursquare-open-places";

    private final FoursquareOpenPlacesFileReader reader;
    private final PlaceImportService importService;

    public FoursquareOpenPlacesImporter(
        FoursquareOpenPlacesFileReader reader,
        PlaceImportService importService
    ) {
        this.reader = reader;
        this.importService = importService;
    }

    public PlaceImportReport importSnapshot(Path path) throws IOException {
        return importService.importSnapshot(SOURCE, reader.read(path));
    }
}
