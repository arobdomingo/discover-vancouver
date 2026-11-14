package com.discovervancouver.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.discovervancouver.model.DataQuality;
import com.discovervancouver.model.Place;

@Component
public class RecommendationPolicy {

    public String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "food", "dining", "eat" -> "restaurant";
            case "coffee" -> "cafe";
            case "night life" -> "nightlife";
            default -> normalized;
        };
    }

    public boolean categoryMatches(Place place, String category) {
        return category == null
            || place.getCategory().equalsIgnoreCase(category);
    }

    public boolean budgetCompatible(Place place, String preference) {
        Integer maximum = maximumPrice(preference);
        return maximum == null
            || !place.isPriceKnown()
            || place.getPriceLevel() <= maximum;
    }

    public int budgetPriority(Place place, String preference) {
        Integer maximum = maximumPrice(preference);
        if (maximum == null) return 0;
        if (!place.isPriceKnown()) return 1;
        return place.getPriceLevel() <= maximum ? 2 : 0;
    }

    public Integer maximumPrice(String preference) {
        if (preference == null || preference.isBlank()) return null;
        return switch (preference.trim().toLowerCase(Locale.ROOT)) {
            case "free" -> 0;
            case "low" -> 1;
            case "medium" -> 2;
            case "high" -> 3;
            default -> null;
        };
    }

    public int dataQualityScore(Place place, int maximum) {
        DataQuality quality = place.getDataQuality();
        if (quality == null) quality = DataQuality.UNKNOWN;
        return switch (quality) {
            case VERIFIED -> maximum;
            case HIGH -> Math.max(0, maximum - 2);
            case MEDIUM -> Math.max(0, maximum / 2);
            case LOW -> Math.min(1, maximum);
            case UNKNOWN -> 0;
        };
    }

    public int completenessScore(Place place, int maximum) {
        int facts = 0;
        if (place.isHoursKnown()) facts += 3;
        if (place.isPriceKnown()) facts++;
        if (place.isIndoorKnown()) facts++;
        if (place.getAddress() != null && !place.getAddress().isBlank()) facts++;
        return maximum * facts / 6;
    }
}
