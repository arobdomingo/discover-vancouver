package com.discovervancouver.importer;

import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.discovervancouver.config.PlaceSourceProperties;

@Component
@Order(110)
public class PlaceSourceCoordinator implements ApplicationRunner {

    private static final Logger logger =
        LoggerFactory.getLogger(PlaceSourceCoordinator.class);

    private final PlaceSourceProperties properties;
    private final FoursquareOpenPlacesImporter foursquareImporter;
    private final VancouverOpenDataImporter vancouverImporter;

    public PlaceSourceCoordinator(
        PlaceSourceProperties properties,
        FoursquareOpenPlacesImporter foursquareImporter,
        VancouverOpenDataImporter vancouverImporter
    ) {
        this.properties = properties;
        this.foursquareImporter = foursquareImporter;
        this.vancouverImporter = vancouverImporter;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (properties.isImportOnStartup()) {
            refreshConfiguredSources();
        }
    }

    @Scheduled(
        fixedDelayString = "${app.place-sources.refresh-delay:PT168H}",
        initialDelayString = "${app.place-sources.initial-delay:PT5M}"
    )
    public void refreshConfiguredSources() {
        importFoursquare();
        importVancouver();
    }

    private void importFoursquare() {
        PlaceSourceProperties.Foursquare config = properties.getFoursquare();
        if (!config.isEnabled() || config.getFile().isBlank()) return;
        try {
            PlaceImportReport report = foursquareImporter.importSnapshot(
                Path.of(config.getFile())
            );
            logReport(FoursquareOpenPlacesImporter.SOURCE, report);
        } catch (Exception exception) {
            logger.error("Foursquare Open Places refresh failed", exception);
        }
    }

    private void importVancouver() {
        if (!properties.getVancouver().isEnabled()) return;
        try {
            for (Map.Entry<VancouverDataset, PlaceImportReport> entry
                : vancouverImporter.importAll().entrySet()) {
                logReport("vancouver-" + entry.getKey().id(), entry.getValue());
            }
        } catch (Exception exception) {
            logger.error("Vancouver Open Data refresh failed", exception);
        }
    }

    private void logReport(String source, PlaceImportReport report) {
        logger.info(
            "Place source refresh source={} created={} updated={} rejected={} stale={} duplicatesLinked={}",
            source,
            report.created(),
            report.updated(),
            report.rejected(),
            report.staleMarked(),
            report.duplicatesLinked()
        );
        report.errors().stream().limit(20).forEach(error ->
            logger.warn("Place source rejection source={} error={}", source, error)
        );
    }
}
