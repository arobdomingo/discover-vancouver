package com.discovervancouver.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import com.discovervancouver.model.OpeningPeriod;

public record PlaceResponse(
    String id,
    String name,
    String category,
    String description,
    String address,
    int priceLevel,
    boolean indoor,
    boolean openNow,
    Map<DayOfWeek, List<OpeningPeriod>> openingHours,
    double latitude,
    double longitude,
    List<String> tags,
    boolean priceKnown,
    boolean indoorKnown,
    boolean hoursKnown,
    String source
) {
}
