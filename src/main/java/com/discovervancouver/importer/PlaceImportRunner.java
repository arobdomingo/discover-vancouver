package com.discovervancouver.importer;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class PlaceImportRunner implements ApplicationRunner {

    private static final Logger logger =
        LoggerFactory.getLogger(PlaceImportRunner.class);

    private final PlaceImportFileReader fileReader;
    private final PlaceImportService importService;
    private final String importFile;
    private final String importSource;

    public PlaceImportRunner(
        PlaceImportFileReader fileReader,
        PlaceImportService importService,
        @Value("${app.import.file:}") String importFile,
        @Value("${app.import.source:}") String importSource
    ) {
        this.fileReader = fileReader;
        this.importService = importService;
        this.importFile = importFile;
        this.importSource = importSource;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (importFile.isBlank()) {
            return;
        }
        if (importSource.isBlank()) {
            throw new IllegalArgumentException(
                "app.import.source is required when app.import.file is set"
            );
        }

        List<PlaceImportRecord> records = fileReader.read(
            Path.of(importFile)
        );
        PlaceImportReport report = importService.importRecords(
            importSource,
            records
        );
        logger.info(
            "Place import complete: {} created, {} updated, {} rejected",
            report.created(),
            report.updated(),
            report.rejected()
        );
        report.errors().forEach(error -> logger.warn(
            "Place import rejection: {}",
            error
        ));
    }
}
