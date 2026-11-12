package com.discovervancouver.importer;

import java.util.List;

public record PlaceImportReport(
    int created,
    int updated,
    int rejected,
    List<String> errors
) {
}
