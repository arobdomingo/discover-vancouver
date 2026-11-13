package com.discovervancouver.importer;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.discovervancouver.config.PlaceSourceProperties;

@Service
public class PlaceSourceCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(PlaceSourceCoordinator.class);
    private final PlaceSourceProperties properties;
    private final FoursquareOpenPlacesImporter foursquareImporter;
    private final VancouverOpenDataImporter vancouverImporter;
    private final PlaceImportMetrics metrics;

    public PlaceSourceCoordinator(PlaceSourceProperties properties,
        FoursquareOpenPlacesImporter foursquareImporter,
        VancouverOpenDataImporter vancouverImporter, PlaceImportMetrics metrics) {
        this.properties = properties;
        this.foursquareImporter = foursquareImporter;
        this.vancouverImporter = vancouverImporter;
        this.metrics = metrics;
    }

    public PlaceSourceRefreshResult refreshConfiguredSources() {
        Map<String, PlaceImportReport> reports = new LinkedHashMap<>();
        Map<String, String> failures = new LinkedHashMap<>();
        importFoursquare(reports, failures);
        importVancouver(reports, failures);
        return new PlaceSourceRefreshResult(reports, failures);
    }

    private void importFoursquare(Map<String, PlaceImportReport> reports,
        Map<String, String> failures) {
        PlaceSourceProperties.Foursquare config = properties.getFoursquare();
        if (!config.isEnabled()) return;
        String source = FoursquareOpenPlacesImporter.SOURCE;
        if (config.getFile().isBlank()) {
            recordFailure(source, new IllegalStateException(
                "FOURSQUARE_OPEN_PLACES_FILE is required"), failures);
            return;
        }
        Instant started = Instant.now();
        try {
            PlaceImportReport report = foursquareImporter.importSnapshot(Path.of(config.getFile()));
            reports.put(source, report);
            recordSuccess(source, report, started);
        } catch (Exception exception) {
            recordFailure(source, exception, failures);
        }
    }

    private void importVancouver(Map<String, PlaceImportReport> reports,
        Map<String, String> failures) {
        if (!properties.getVancouver().isEnabled()) return;
        Instant started = Instant.now();
        VancouverImportResult result = vancouverImporter.importAll();
        result.reports().forEach((dataset, report) -> {
            String source = "vancouver-" + dataset.id();
            reports.put(source, report);
            recordSuccess(source, report, started);
        });
        result.failures().forEach((dataset, message) -> {
            String source = "vancouver-" + dataset.id();
            failures.put(source, message);
            metrics.recordFailure(source);
        });
    }

    private void recordSuccess(String source, PlaceImportReport report, Instant started) {
        metrics.recordSuccess(source, report, Duration.between(started, Instant.now()));
        logger.info("Place source refresh source={} created={} updated={} rejected={} stale={} duplicatesLinked={}",
            source, report.created(), report.updated(), report.rejected(),
            report.staleMarked(), report.duplicatesLinked());
        report.errors().stream().limit(20).forEach(error ->
            logger.warn("Place source rejection source={} error={}", source, error));
    }

    private void recordFailure(String source, Exception exception,
        Map<String, String> failures) {
        String message = exception.getMessage() == null
            ? exception.getClass().getSimpleName() : exception.getMessage();
        failures.put(source, message);
        metrics.recordFailure(source);
        logger.error("Place source refresh failed source={}", source, exception);
    }
}
