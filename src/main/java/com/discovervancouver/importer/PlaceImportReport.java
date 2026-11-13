package com.discovervancouver.importer;

import java.util.List;

public record PlaceImportReport(
    int created,
    int updated,
    int rejected,
    int staleMarked,
    int duplicatesLinked,
    List<String> errors
) {
    public PlaceImportReport(
        int created,
        int updated,
        int rejected,
        List<String> errors
    ) {
        this(created, updated, rejected, 0, 0, errors);
    }
}
