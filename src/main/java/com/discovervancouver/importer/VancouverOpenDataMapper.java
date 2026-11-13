package com.discovervancouver.importer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.discovervancouver.model.DataQuality;
import com.discovervancouver.model.PlaceOperatingStatus;

@Component
public class VancouverOpenDataMapper {

    public Optional<PlaceImportRecord> map(
        VancouverDataset dataset,
        Map<String, Object> raw,
        List<String> relatedTags
    ) {
        Map<String, Object> values = normalizeKeys(raw);
        String name = first(values,
            "name", "sitename", "businessname", "vendorname",
            "programname", "organizationname", "spacename",
            "culturalspacename", "description", "location"
        );
        Coordinate coordinate = coordinate(values);
        if (name.isBlank() || coordinate == null) {
            return Optional.empty();
        }

        String address = address(values);
        String id = first(values,
            "parkid", "siteid", "businessid", "vendorid", "programid",
            "libraryid", "communitycentreid", "culturalspaceid", "id", "key"
        );
        if (id.isBlank()) {
            id = UUID.nameUUIDFromBytes((dataset.id() + "|" + name + "|"
                + address + "|" + coordinate.latitude + "|"
                + coordinate.longitude).getBytes(StandardCharsets.UTF_8))
                .toString();
        }

        Set<String> tags = new LinkedHashSet<>();
        tags.add(dataset.category());
        tags.add(dataset.indoor() ? "indoor" : "outdoor");
        if (dataset.priceKnown()) tags.add("free");
        addTag(tags, first(values,
            "primaryuse", "type", "businesstype", "facilitytype",
            "localarea", "neighbourhoodname", "programtype"
        ));
        relatedTags.forEach(tag -> addTag(tags, tag));

        String source = "vancouver-" + dataset.id();
        return Optional.of(new PlaceImportRecord(
            id,
            name,
            dataset.category(),
            description(dataset, name, values),
            address.isBlank() ? "Vancouver, BC" : address,
            0,
            dataset.indoor(),
            Map.of(),
            coordinate.latitude,
            coordinate.longitude,
            new ArrayList<>(tags),
            "https://opendata.vancouver.ca/explore/dataset/"
                + dataset.id() + "/",
            extractInstant(values),
            PlaceOperatingStatus.OPEN,
            DataQuality.VERIFIED,
            dataset.priceKnown(),
            true,
            Map.of(
                "name", source,
                "address", source,
                "coordinates", source,
                "category", source
            )
        ));
    }

    public String sourceId(Map<String, Object> raw) {
        return first(normalizeKeys(raw), "parkid", "siteid", "id");
    }

    public String facilityTag(Map<String, Object> raw) {
        Map<String, Object> values = normalizeKeys(raw);
        return first(values, "facilitytype", "facility", "name", "type");
    }

    private String description(
        VancouverDataset dataset,
        String name,
        Map<String, Object> values
    ) {
        String detail = first(values,
            "description", "primaryuse", "programdescription", "type"
        );
        if (!detail.isBlank()) return detail;
        return name + " is an official " + dataset.category()
            + " location published by the City of Vancouver.";
    }

    private String address(Map<String, Object> values) {
        String direct = first(values,
            "address", "streetaddress", "siteaddress", "location"
        );
        if (!direct.isBlank()) return direct;
        String number = first(values, "streetnumber", "housenumber");
        String street = first(values, "streetname", "street");
        String combined = (number + " " + street).trim();
        return combined.isBlank() ? "" : combined + ", Vancouver, BC";
    }

    private Coordinate coordinate(Map<String, Object> values) {
        Double latitude = decimal(values, "latitude", "lat");
        Double longitude = decimal(values, "longitude", "lon", "lng");
        if (latitude != null && longitude != null) {
            return valid(latitude, longitude);
        }
        for (String key : List.of(
            "geopoint2d", "googlemapdest", "geom", "geometry"
        )) {
            Object geo = values.get(key);
            if (geo instanceof Map<?, ?> map) {
                Coordinate coordinate = coordinateFromMap(map);
                if (coordinate != null) return coordinate;
            }
        }
        String destination = first(values, "googlemapdest");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "lat[^-0-9]*(-?[0-9.]+).*lon[^-0-9]*(-?[0-9.]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(destination);
        if (matcher.find()) {
            return valid(
                Double.parseDouble(matcher.group(1)),
                Double.parseDouble(matcher.group(2))
            );
        }
        return null;
    }

    private Coordinate coordinateFromMap(Map<?, ?> map) {
        Object lat = map.get("lat");
        Object lon = map.get("lon");
        if (lat instanceof Number latNumber && lon instanceof Number lonNumber) {
            return valid(latNumber.doubleValue(), lonNumber.doubleValue());
        }
        Object coordinates = map.get("coordinates");
        if (coordinates instanceof List<?> pair && pair.size() >= 2
            && pair.get(0) instanceof Number x
            && pair.get(1) instanceof Number y) {
            return valid(y.doubleValue(), x.doubleValue());
        }
        Object geometry = map.get("geometry");
        if (geometry instanceof Map<?, ?> nested) {
            return coordinateFromMap(nested);
        }
        return null;
    }

    private Coordinate valid(double latitude, double longitude) {
        if (latitude < 48.9 || latitude > 49.6
            || longitude < -123.6 || longitude > -122.4) {
            return null;
        }
        return new Coordinate(latitude, longitude);
    }

    private Double decimal(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number number) return number.doubleValue();
            if (value != null) {
                try { return Double.parseDouble(value.toString()); }
                catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }

    private Instant extractInstant(Map<String, Object> values) {
        String value = first(values, "extractdate", "lastupdated", "daterefreshed");
        if (value.isBlank()) return null;
        try { return Instant.parse(value); }
        catch (RuntimeException ignored) { return null; }
    }

    private Map<String, Object> normalizeKeys(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, value) -> normalized.put(
            key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""),
            value
        ));
        return normalized;
    }

    private String first(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private void addTag(Set<String> tags, String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
            .replace("&", "and")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (!normalized.isBlank()) tags.add(normalized);
    }

    private record Coordinate(double latitude, double longitude) { }
}
