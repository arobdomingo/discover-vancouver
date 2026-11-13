package com.discovervancouver.importer;

import java.time.Instant;
import java.util.Map;

public record PlaceDataQualityReport(
    Instant generatedAt,
    long total,
    long discoverable,
    long stale,
    long closed,
    long unknownHours,
    long unknownPrice,
    long unknownIndoor,
    Map<String, Long> bySource,
    Map<String, Long> byQuality
) {
}
