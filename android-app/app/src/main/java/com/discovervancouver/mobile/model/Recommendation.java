package com.discovervancouver.mobile.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class Recommendation implements Serializable {

    private final String placeId;
    private final String name;
    private final String category;
    private final String description;
    private final String address;
    private final int priceLevel;
    private final boolean indoor;
    private final boolean openNow;
    private final List<String> tags;
    private final int score;
    private final int distanceMeters;
    private final int walkingMinutes;
    private final double latitude;
    private final double longitude;
    private final boolean rainyWeather;
    private final List<String> reasons;
    private final boolean priceKnown;
    private final boolean indoorKnown;
    private final boolean hoursKnown;
    private final String source;

    public Recommendation(
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
        this.placeId = placeId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.address = address;
        this.priceLevel = priceLevel;
        this.indoor = indoor;
        this.openNow = openNow;
        this.tags = List.copyOf(tags);
        this.score = score;
        this.distanceMeters = distanceMeters;
        this.walkingMinutes = walkingMinutes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rainyWeather = rainyWeather;
        this.reasons = List.copyOf(reasons);
        this.priceKnown = priceKnown;
        this.indoorKnown = indoorKnown;
        this.hoursKnown = hoursKnown;
        this.source = source;
    }

    public Recommendation(
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
        List<String> reasons
    ) {
        this(
            placeId, name, category, description, address, priceLevel,
            indoor, openNow, tags, score, distanceMeters, walkingMinutes,
            latitude, longitude, rainyWeather, reasons,
            true, true, true, null
        );
    }

    public static Recommendation fromJson(JSONObject json) throws JSONException {
        return new Recommendation(
            json.getString("placeId"),
            json.getString("name"),
            json.getString("category"),
            json.getString("description"),
            json.getString("address"),
            json.getInt("priceLevel"),
            json.getBoolean("indoor"),
            json.getBoolean("openNow"),
            stringsFrom(json.optJSONArray("tags")),
            json.getInt("score"),
            json.getInt("distanceMeters"),
            json.getInt("walkingMinutes"),
            json.getDouble("latitude"),
            json.getDouble("longitude"),
            json.getBoolean("rainyWeather"),
            stringsFrom(json.optJSONArray("reasons")),
            json.optBoolean("priceKnown", true),
            json.optBoolean("indoorKnown", true),
            json.optBoolean("hoursKnown", true),
            json.optString("source", null)
        );
    }

    private static List<String> stringsFrom(JSONArray array)
        throws JSONException {
        if (array == null) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            values.add(array.getString(index));
        }
        return values;
    }

    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public int getPriceLevel() { return priceLevel; }
    public boolean isIndoor() { return indoor; }
    public boolean isOpenNow() { return openNow; }
    public List<String> getTags() { return tags; }
    public int getScore() { return score; }
    public int getDistanceMeters() { return distanceMeters; }
    public int getWalkingMinutes() { return walkingMinutes; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isRainyWeather() { return rainyWeather; }
    public List<String> getReasons() { return reasons; }
    public boolean isPriceKnown() { return priceKnown; }
    public boolean isIndoorKnown() { return indoorKnown; }
    public boolean isHoursKnown() { return hoursKnown; }
    public String getSource() { return source; }
}
