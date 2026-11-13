package com.discovervancouver.importer;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PlaceImportMetrics {
    private final MeterRegistry registry;

    public PlaceImportMetrics(MeterRegistry registry) { this.registry = registry; }

    public void recordSuccess(String source, PlaceImportReport report, Duration duration) {
        increment(source, "created", report.created());
        increment(source, "updated", report.updated());
        increment(source, "rejected", report.rejected());
        increment(source, "stale", report.staleMarked());
        increment(source, "duplicates_linked", report.duplicatesLinked());
        Timer.builder("discover.place.import.duration")
            .description("Duration of a place-source refresh")
            .tag("source", source).tag("outcome", "success")
            .register(registry).record(duration);
        registry.counter("discover.place.import.runs", "source", source,
            "outcome", "success").increment();
    }

    public void recordFailure(String source) {
        registry.counter("discover.place.import.runs", "source", source,
            "outcome", "failure").increment();
    }

    private void increment(String source, String outcome, int amount) {
        registry.counter("discover.place.import.records", "source", source,
            "outcome", outcome).increment(amount);
    }
}
