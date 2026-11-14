package com.discovervancouver.mobile.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record RecommendationQuery(
    double latitude,
    double longitude,
    String category,
    Boolean indoorPreferred,
    int maxWalkingMinutes,
    String budgetPreference,
    String atmosphere,
    boolean openOnly,
    int limit
) {

    public String toQueryString() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("lat", Double.toString(latitude));
        parameters.put("lng", Double.toString(longitude));
        putWhenPresent(parameters, "category", apiCategory(category));
        if (indoorPreferred != null) {
            parameters.put(
                "indoorPreferred",
                indoorPreferred.toString()
            );
        }
        parameters.put(
            "maxWalkingMinutes",
            Integer.toString(maxWalkingMinutes)
        );
        putWhenPresent(
            parameters,
            "budgetPreference",
            budgetPreference
        );
        putWhenPresent(parameters, "atmosphere", atmosphere);
        parameters.put("openOnly", Boolean.toString(openOnly));
        parameters.put("limit", Integer.toString(limit));

        return parameters.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    }

    private static void putWhenPresent(
        Map<String, String> parameters,
        String name,
        String value
    ) {
        if (value != null && !value.isBlank()) {
            parameters.put(name, value);
        }
    }

    private static String apiCategory(String category) {
        if (category == null) return null;
        return switch (category.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "food" -> "restaurant";
            default -> category.trim().toLowerCase(java.util.Locale.ROOT);
        };
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8.name()
            );
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException(
                "UTF-8 encoding is unavailable",
                exception
            );
        }
    }
}
