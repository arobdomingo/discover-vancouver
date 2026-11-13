package com.discovervancouver.importer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class FoursquareCategoryMapper {

    public MappedCategory map(List<String> sourceCategories) {
        String joined = String.join(" ", sourceCategories)
            .toLowerCase(Locale.ROOT);
        String category;
        boolean indoor;
        boolean indoorKnown = true;

        if (containsAny(joined, "coffee", "cafe", "tea room", "bakery")) {
            category = "cafe";
            indoor = true;
        } else if (containsAny(joined, "restaurant", "food", "diner")) {
            category = "restaurant";
            indoor = true;
        } else if (containsAny(joined, "park", "garden", "beach", "trail")) {
            category = "park";
            indoor = false;
        } else if (containsAny(joined, "museum", "gallery", "theater", "theatre", "arts")) {
            category = "culture";
            indoor = true;
        } else if (containsAny(joined, "library", "study")) {
            category = "study";
            indoor = true;
        } else if (containsAny(joined, "bar", "pub", "nightclub", "music venue")) {
            category = "nightlife";
            indoor = true;
        } else if (containsAny(joined, "shop", "store", "mall", "market")) {
            category = "shopping";
            indoor = true;
        } else if (containsAny(joined, "gym", "recreation", "sports", "pool")) {
            category = "recreation";
            indoor = !joined.contains("field") && !joined.contains("outdoor");
        } else if (containsAny(
            joined,
            "attraction",
            "landmark",
            "historic",
            "tourist",
            "monument",
            "zoo",
            "aquarium",
            "scenic",
            "viewpoint"
        )) {
            category = "attraction";
            indoor = false;
            indoorKnown = false;
        } else {
            category = "attraction";
            indoor = false;
            indoorKnown = false;
        }

        Set<String> tags = new LinkedHashSet<>();
        tags.add(category);
        sourceCategories.stream()
            .map(this::normalizeTag)
            .filter(value -> !value.isBlank())
            .limit(12)
            .forEach(tags::add);
        if (indoorKnown) {
            tags.add(indoor ? "indoor" : "outdoor");
        }
        if (category.equals("park") || category.equals("study")) {
            tags.add("free");
        }

        return new MappedCategory(
            category,
            indoor,
            indoorKnown,
            new ArrayList<>(tags)
        );
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTag(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
            .replace("&", "and")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (normalized.length() <= 50) {
            return normalized;
        }
        return normalized.substring(0, 50).replaceAll("-+$", "");
    }

    public record MappedCategory(
        String category,
        boolean indoor,
        boolean indoorKnown,
        List<String> tags
    ) { }
}
