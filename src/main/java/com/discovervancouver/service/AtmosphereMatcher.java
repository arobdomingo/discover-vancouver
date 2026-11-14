package com.discovervancouver.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.discovervancouver.model.Place;

@Component
public class AtmosphereMatcher {

    public boolean matches(Place place, String requestedAtmosphere) {
        String atmosphere = normalize(requestedAtmosphere);
        if (atmosphere.isBlank()) return false;
        Set<String> tags = place.getTags().stream()
            .map(this::normalize)
            .collect(java.util.stream.Collectors.toSet());
        if (tags.contains(atmosphere)) return true;

        return switch (atmosphere) {
            case "quiet", "calm" -> place.getCategory().equals("study")
                || hasAny(tags, "library", "bookstore", "botanical-garden");
            case "social", "lively" -> place.getCategory().equals("nightlife")
                || hasAny(tags, "community-centre", "music-venue", "pub", "lounge");
            case "outdoor", "outdoors" -> place.isIndoorKnown()
                ? !place.isIndoor()
                : place.getCategory().equals("park");
            case "active", "energetic" -> place.getCategory().equals("recreation")
                || hasAny(tags, "gym", "pool", "hiking-trail", "sports");
            case "romantic" -> hasAny(tags,
                "wine-bar", "cocktail-bar", "scenic-lookout", "fine-dining");
            case "family", "family-friendly" -> hasAny(tags,
                "playground", "zoo", "aquarium", "community-centre", "family");
            case "late-night" -> hasAny(tags, "late-night", "nightclub");
            default -> false;
        };
    }

    private boolean hasAny(Set<String> tags, String... values) {
        return List.of(values).stream().anyMatch(value ->
            tags.stream().anyMatch(tag -> tag.equals(value)
                || tag.startsWith(value + "-")
                || tag.endsWith("-" + value)
                || tag.contains("-" + value + "-"))
        );
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
            .replace("&", "and")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }
}
