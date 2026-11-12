package com.discovervancouver.mobile.api;

import com.discovervancouver.mobile.model.Recommendation;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class RecommendationJsonParser {

    public List<Recommendation> parse(String body) throws JSONException {
        JSONArray array = new JSONArray(body);
        List<Recommendation> recommendations = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            recommendations.add(
                Recommendation.fromJson(array.getJSONObject(index))
            );
        }
        return List.copyOf(recommendations);
    }
}
