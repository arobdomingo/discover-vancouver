package com.discovervancouver.importer;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import com.discovervancouver.model.Place;
import com.discovervancouver.repository.PlaceRepository;

@Service
public class PlaceDeduplicationService {

    static final double MATCH_RADIUS_KILOMETRES = 0.075;

    private final PlaceRepository placeRepository;

    public PlaceDeduplicationService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public MatchResult canonicalize(Place imported, Place existing) {
        if (existing != null
            && existing.getCanonicalPlaceId() != null
            && !existing.getCanonicalPlaceId().isBlank()) {
            return new MatchResult(existing.getCanonicalPlaceId(), false);
        }

        List<Place> nearby = placeRepository.findByLocationNear(
            new GeoJsonPoint(imported.getLongitude(), imported.getLatitude()),
            new Distance(MATCH_RADIUS_KILOMETRES, Metrics.KILOMETERS)
        );
        if (nearby != null) {
            for (Place candidate : nearby) {
                if (existing != null && candidate.getId().equals(existing.getId())) {
                    continue;
                }
                if (isHighConfidenceMatch(imported, candidate)) {
                    candidate.ensureCanonicalPlaceId();
                    if (candidate.getId() != null) {
                        placeRepository.save(candidate);
                    }
                    return new MatchResult(candidate.getCanonicalPlaceId(), true);
                }
            }
        }

        return new MatchResult(UUID.randomUUID().toString(), false);
    }

    boolean isHighConfidenceMatch(Place first, Place second) {
        String firstName = normalize(first.getName());
        String secondName = normalize(second.getName());
        if (!firstName.isBlank() && firstName.equals(secondName)) {
            return true;
        }

        String firstAddress = normalize(first.getAddress());
        String secondAddress = normalize(second.getAddress());
        return !firstAddress.isBlank()
            && firstAddress.equals(secondAddress)
            && tokenSimilarity(firstName, secondName) >= 0.6;
    }

    private double tokenSimilarity(String first, String second) {
        Set<String> left = tokens(first);
        Set<String> right = tokens(second);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokens(String value) {
        return new HashSet<>(Arrays.stream(value.split("\\s+"))
            .filter(token -> token.length() > 1)
            .toList());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replace("&", " and ")
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }

    public record MatchResult(String canonicalPlaceId, boolean duplicateLinked) {
    }
}
