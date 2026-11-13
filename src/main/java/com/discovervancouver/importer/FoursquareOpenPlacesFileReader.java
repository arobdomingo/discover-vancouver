package com.discovervancouver.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.discovervancouver.config.PlaceSourceProperties;
import com.discovervancouver.model.DataQuality;
import com.discovervancouver.model.PlaceOperatingStatus;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class FoursquareOpenPlacesFileReader {

    private final ObjectMapper objectMapper;
    private final FoursquareCategoryMapper categoryMapper;
    private final PlaceSourceProperties properties;

    public FoursquareOpenPlacesFileReader(
        ObjectMapper objectMapper,
        FoursquareCategoryMapper categoryMapper,
        PlaceSourceProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.categoryMapper = categoryMapper;
        this.properties = properties;
    }

    public List<PlaceImportRecord> read(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        List<Map<String, Object>> sourceRecords;
        if (content.startsWith("[")) {
            sourceRecords = objectMapper.readValue(
                content,
                new TypeReference<List<Map<String, Object>>>() { }
            );
        } else {
            sourceRecords = new ArrayList<>();
            for (String line : content.lines().toList()) {
                if (!line.isBlank()) {
                    sourceRecords.add(objectMapper.readValue(
                        line,
                        new TypeReference<Map<String, Object>>() { }
                    ));
                }
            }
        }

        return sourceRecords.stream()
            .map(this::mapRecord)
            .filter(record -> record != null)
            .toList();
    }

    private PlaceImportRecord mapRecord(Map<String, Object> values) {
        String id = string(values, "fsq_place_id");
        String name = string(values, "name");
        double latitude = number(values, "latitude");
        double longitude = number(values, "longitude");
        if (id.isBlank() || name.isBlank() || !insideArea(latitude, longitude)) {
            return null;
        }

        Instant closedAt = instant(values.get("date_closed"));
        if (closedAt != null || hasBlockingFlag(values.get("unresolved_flags"))) {
            return null;
        }
        Instant refreshedAt = instant(values.get("date_refreshed"));
        List<String> categories = stringList(
            values.get("fsq_category_labels")
        );
        if (categories.isEmpty()) {
            categories = hierarchyCategories(values);
        }
        FoursquareCategoryMapper.MappedCategory mapped =
            categoryMapper.map(categories);
        String address = buildAddress(values);
        if (address.isBlank()) {
            address = "Metro Vancouver, BC";
        }

        return new PlaceImportRecord(
            id,
            name,
            mapped.category(),
            description(name, categories),
            address,
            0,
            mapped.indoor(),
            Map.of(),
            latitude,
            longitude,
            mapped.tags(),
            "https://foursquare.com/placemakers/review-place/" + id,
            refreshedAt,
            PlaceOperatingStatus.OPEN,
            quality(refreshedAt),
            false,
            mapped.indoorKnown(),
            Map.of(
                "name", "foursquare-open-places",
                "address", "foursquare-open-places",
                "coordinates", "foursquare-open-places",
                "category", "foursquare-open-places"
            )
        );
    }

    private List<String> hierarchyCategories(Map<String, Object> values) {
        List<String> categories = new ArrayList<>();
        for (int level = 1; level <= 6; level++) {
            String category = string(values, "level" + level + "_category_name");
            if (!category.isBlank()) {
                categories.add(category);
            }
        }
        return categories;
    }

    private String description(String name, List<String> categories) {
        String type = categories.isEmpty() ? "place" : categories.get(categories.size() - 1);
        return name + " is listed as a " + type + " in Foursquare Open Places.";
    }

    private String buildAddress(Map<String, Object> values) {
        return List.of(
                string(values, "address"),
                string(values, "locality"),
                string(values, "region"),
                string(values, "postcode")
            ).stream()
            .filter(value -> !value.isBlank())
            .distinct()
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    private boolean insideArea(double latitude, double longitude) {
        PlaceSourceProperties.Foursquare config = properties.getFoursquare();
        return latitude >= config.getMinLatitude()
            && latitude <= config.getMaxLatitude()
            && longitude >= config.getMinLongitude()
            && longitude <= config.getMaxLongitude();
    }

    private DataQuality quality(Instant refreshedAt) {
        if (refreshedAt == null) {
            return DataQuality.UNKNOWN;
        }
        long age = ChronoUnit.DAYS.between(refreshedAt, Instant.now());
        if (age <= 365) return DataQuality.HIGH;
        if (age <= 730) return DataQuality.MEDIUM;
        return DataQuality.LOW;
    }

    private String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private double number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value == null ? "NaN" : value.toString());
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private boolean hasBlockingFlag(Object value) {
        Set<String> blockingFlags = Set.of(
            "closed",
            "duplicate",
            "delete",
            "privatevenue",
            "inappropriate",
            "doesnt_exist"
        );
        return stringList(value).stream()
            .map(flag -> flag.trim().toLowerCase(java.util.Locale.ROOT))
            .anyMatch(blockingFlags::contains);
    }

    private Instant instant(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            return Instant.parse(value.toString());
        } catch (RuntimeException exception) {
            try {
                return LocalDate.parse(value.toString())
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }
}
