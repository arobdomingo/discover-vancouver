package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;

import com.discovervancouver.mobile.TestRecommendations;
import com.discovervancouver.mobile.model.Recommendation;

import org.junit.Test;

import java.util.List;

public class RecommendationMapModelTest {

    @Test
    public void mappableRemovesDuplicatePlaceIds() {
        Recommendation recommendation = TestRecommendations.sample();

        List<Recommendation> results = RecommendationMapModel.mappable(
            List.of(recommendation, recommendation)
        );

        assertEquals(1, results.size());
    }

    @Test
    public void markerColorsDistinguishCategories() {
        assertEquals("#A85A2A", RecommendationMapModel.markerColor("cafe"));
        assertEquals("#2E7D32", RecommendationMapModel.markerColor("park"));
        assertEquals("#A12662", RecommendationMapModel.markerColor("nightlife"));
        assertEquals(
            RecommendationMapModel.DEFAULT_MARKER_COLOR,
            RecommendationMapModel.markerColor("other")
        );
    }
}
