package com.discovervancouver.dto;

import java.util.List;

public record RecommendationResponse(
    String placeId,
    String name,
    String category,
    String description,
    String address,
    int priceLevel,
    boolean indoor,
    boolean openNow,
    List<String> tags,
    int score,
    int distanceMeters,
    int walkingMinutes,
    double latitude,
    double longitude,
    boolean rainyWeather,
    List<String> reasons,
    boolean priceKnown,
    boolean indoorKnown,
    boolean hoursKnown,
    String source
) {
    
}
