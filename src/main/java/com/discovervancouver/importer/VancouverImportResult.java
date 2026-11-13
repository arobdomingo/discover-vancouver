package com.discovervancouver.importer;

import java.util.Map;

public record VancouverImportResult(
    Map<VancouverDataset, PlaceImportReport> reports,
    Map<VancouverDataset, String> failures
) {
    public VancouverImportResult {
        reports = Map.copyOf(reports);
        failures = Map.copyOf(failures);
    }
}
