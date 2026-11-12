package com.discovervancouver.mobile;

import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public final class TestRecommendations {

    private TestRecommendations() { }

    public static Recommendation sample() {
        return new Recommendation(
            "place-1",
            "Burrard Coffee",
            "cafe",
            "A quiet downtown cafe",
            "100 Burrard Street",
            1,
            true,
            true,
            List.of("quiet", "rainy-day"),
            91,
            350,
            5,
            49.2827,
            -123.1207,
            false,
            List.of("Matches your cafe preference", "About 5 minutes away")
        );
    }
}
