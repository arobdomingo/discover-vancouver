package com.discovervancouver.importer;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.discovervancouver.model.Place;
import com.discovervancouver.model.PlaceOperatingStatus;
import com.discovervancouver.repository.PlaceRepository;

@Service
public class PlaceDataQualityService {
    private final PlaceRepository repository;
    private final Clock clock;

    public PlaceDataQualityService(PlaceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PlaceDataQualityReport generate() {
        List<Place> places = repository.findAll();
        Map<String, Long> bySource = countBy(places, place ->
            label(place.getSource(), "unattributed"));
        Map<String, Long> byQuality = countBy(places, place ->
            place.getDataQuality() == null ? "UNKNOWN" : place.getDataQuality().name());
        return new PlaceDataQualityReport(
            Instant.now(clock),
            places.size(),
            places.stream().filter(Place::isDiscoverable).count(),
            places.stream().filter(Place::isStale).count(),
            places.stream().filter(place -> place.getOperatingStatus()
                == PlaceOperatingStatus.PERMANENTLY_CLOSED
                || place.getOperatingStatus() == PlaceOperatingStatus.TEMPORARILY_CLOSED).count(),
            places.stream().filter(place -> !place.isHoursKnown()).count(),
            places.stream().filter(place -> !place.isPriceKnown()).count(),
            places.stream().filter(place -> !place.isIndoorKnown()).count(),
            bySource,
            byQuality
        );
    }

    private Map<String, Long> countBy(
        List<Place> places,
        Function<Place, String> classifier
    ) {
        return places.stream().collect(Collectors.groupingBy(
            classifier,
            TreeMap::new,
            Collectors.counting()
        ));
    }

    private String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
