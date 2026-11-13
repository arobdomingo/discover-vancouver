package com.discovervancouver.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.discovervancouver.config.PlaceSourceProperties;

import tools.jackson.databind.ObjectMapper;

@Component
@Profile("ingestion")
@Order(200)
public class PlaceSourceIngestionRunner implements ApplicationRunner {
    private static final Logger logger =
        LoggerFactory.getLogger(PlaceSourceIngestionRunner.class);

    private final PlaceSourceCoordinator coordinator;
    private final PlaceDataQualityService qualityService;
    private final PlaceSourceProperties properties;
    private final ObjectMapper objectMapper;

    public PlaceSourceIngestionRunner(PlaceSourceCoordinator coordinator,
        PlaceDataQualityService qualityService,
        PlaceSourceProperties properties,
        ObjectMapper objectMapper) {
        this.coordinator = coordinator;
        this.qualityService = qualityService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments arguments) throws IOException {
        PlaceSourceRefreshResult result = coordinator.refreshConfiguredSources();
        PlaceDataQualityReport report = qualityService.generate();
        Path reportPath = Path.of(properties.getReportFile()).toAbsolutePath();
        if (reportPath.getParent() != null) {
            Files.createDirectories(reportPath.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(reportPath.toFile(), report);
        logger.info("Place data-quality report written path={} total={} discoverable={} stale={}",
            reportPath, report.total(), report.discoverable(), report.stale());
        if (!result.successful()) {
            throw new IllegalStateException(
                "One or more place sources failed: " + result.failures()
            );
        }
    }
}
