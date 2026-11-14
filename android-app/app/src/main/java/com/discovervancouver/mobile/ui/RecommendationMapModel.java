package com.discovervancouver.mobile.ui;

import com.discovervancouver.mobile.model.Recommendation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RecommendationMapModel {

    static final String DEFAULT_MARKER_COLOR = "#167568";

    private RecommendationMapModel() { }

    static List<Recommendation> mappable(List<Recommendation> values) {
        Map<String, Recommendation> uniquePlaces = new LinkedHashMap<>();
        for (Recommendation value : values) {
            if (value == null || !validCoordinates(value)) {
                continue;
            }
            uniquePlaces.putIfAbsent(value.getPlaceId(), value);
        }
        return List.copyOf(uniquePlaces.values());
    }

    static String markerColor(String category) {
        if (category == null) {
            return DEFAULT_MARKER_COLOR;
        }
        return switch (category.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "cafe" -> "#A85A2A";
            case "restaurant" -> "#D96C1F";
            case "park" -> "#2E7D32";
            case "study" -> "#315BB5";
            case "shopping" -> "#7B3FA1";
            case "nightlife" -> "#A12662";
            default -> DEFAULT_MARKER_COLOR;
        };
    }

    private static boolean validCoordinates(Recommendation value) {
        return Double.isFinite(value.getLatitude())
            && Double.isFinite(value.getLongitude())
            && value.getLatitude() >= -90.0
            && value.getLatitude() <= 90.0
            && value.getLongitude() >= -180.0
            && value.getLongitude() <= 180.0;
    }
}
