package com.discovervancouver.importer;

import java.util.Map;

public record PlaceSourceRefreshResult(
    Map<String, PlaceImportReport> reports,
    Map<String, String> failures
) {
    public PlaceSourceRefreshResult {
        reports = Map.copyOf(reports);
        failures = Map.copyOf(failures);
    }

    public boolean successful() { return failures.isEmpty(); }
}
