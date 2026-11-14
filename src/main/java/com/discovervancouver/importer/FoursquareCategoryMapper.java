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
        Taxonomy taxonomy = Taxonomy.from(sourceCategories);
        String category;
        boolean indoor;
        boolean indoorKnown = true;

        if (isNightlife(taxonomy)) {
            category = "nightlife";
            indoor = true;
        } else if (isCafe(taxonomy)) {
            category = "cafe";
            indoor = true;
        } else if (taxonomy.hasRoot("dining and drinking")) {
            category = "restaurant";
            indoor = true;
        } else if (isPark(taxonomy)) {
            category = "park";
            indoor = false;
        } else if (taxonomy.hasRoot("arts and entertainment")
            || taxonomy.hasAnySegment(
                "museum", "art gallery", "theater", "theatre"
            )) {
            category = "culture";
            indoor = true;
        } else if (taxonomy.hasAnySegment(
            "library", "college and university", "college", "university"
        )) {
            category = "study";
            indoor = true;
        } else if (taxonomy.hasRoot("retail")) {
            category = "shopping";
            indoor = true;
        } else if (taxonomy.hasRoot("sports and recreation")) {
            category = "recreation";
            indoor = !taxonomy.hasAnySegment(
                "athletic field", "baseball field", "soccer field",
                "hiking trail", "ski area"
            );
        } else if (taxonomy.hasRoot("landmarks and outdoors")
            || taxonomy.hasAnySegment(
                "tourist attraction", "historic and protected site",
                "monument", "scenic lookout", "zoo", "aquarium"
            )) {
            category = "attraction";
            indoor = false;
            indoorKnown = false;
        } else {
            category = "other";
            indoor = false;
            indoorKnown = false;
        }

        Set<String> tags = new LinkedHashSet<>();
        tags.add(category);
        taxonomy.segments().stream()
            .map(this::normalizeTag)
            .filter(value -> !value.isBlank())
            .limit(12)
            .forEach(tags::add);
        sourceCategories.stream()
            .map(this::normalizeTag)
            .filter(value -> !value.isBlank())
            .limit(4)
            .forEach(tags::add);
        if (indoorKnown) tags.add(indoor ? "indoor" : "outdoor");
        if (category.equals("park") || category.equals("study")) {
            tags.add("free");
        }

        return new MappedCategory(
            category,
            indoor,
            indoorKnown,
            new ArrayList<>(tags).stream().limit(20).toList()
        );
    }

    private boolean isNightlife(Taxonomy taxonomy) {
        if (!taxonomy.hasRoot("dining and drinking")) return false;
        if (taxonomy.hasAnySegment("juice bar")) return false;
        return taxonomy.hasAnySegment(
            "bar", "pub", "nightclub", "music venue", "lounge",
            "brewery", "beer garden", "karaoke bar", "cocktail bar",
            "wine bar", "sports bar", "dive bar", "hotel bar"
        );
    }

    private boolean isCafe(Taxonomy taxonomy) {
        return taxonomy.hasAnySegment(
            "cafe", "coffee shop", "tea room", "bakery", "juice bar"
        );
    }

    private boolean isPark(Taxonomy taxonomy) {
        return taxonomy.hasRoot("landmarks and outdoors")
            && taxonomy.hasAnySegment(
                "park", "garden", "beach", "hiking trail", "trail",
                "playground", "dog park", "national park"
            );
    }

    private String normalizeTag(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
            .replace("&", "and")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (normalized.length() <= 50) return normalized;
        return normalized.substring(0, 50).replaceAll("-+$", "");
    }

    private record Taxonomy(Set<String> roots, Set<String> segments) {
        static Taxonomy from(List<String> labels) {
            Set<String> roots = new LinkedHashSet<>();
            Set<String> segments = new LinkedHashSet<>();
            for (String label : labels) {
                List<String> path = java.util.Arrays.stream(label.split(">"))
                    .map(Taxonomy::normalize)
                    .filter(value -> !value.isBlank())
                    .toList();
                if (!path.isEmpty()) roots.add(path.getFirst());
                segments.addAll(path);
            }
            return new Taxonomy(roots, segments);
        }

        boolean hasRoot(String root) { return roots.contains(normalize(root)); }

        boolean hasAnySegment(String... candidates) {
            for (String candidate : candidates) {
                if (segments.contains(normalize(candidate))) return true;
            }
            return false;
        }

        private static String normalize(String value) {
            return value.toLowerCase(Locale.ROOT)
                .replace("&", "and")
                .replaceAll("\\s+", " ")
                .trim();
        }
    }

    public record MappedCategory(
        String category,
        boolean indoor,
        boolean indoorKnown,
        List<String> tags
    ) { }
}
