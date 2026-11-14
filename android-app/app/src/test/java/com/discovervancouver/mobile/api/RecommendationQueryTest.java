package com.discovervancouver.mobile.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecommendationQueryTest {

    @Test
    public void queryIncludesSelectedFiltersAndEncodesText() {
        RecommendationQuery query = new RecommendationQuery(
            49.2827,
            -123.1207,
            "cafe",
            true,
            20,
            "low",
            "quiet social",
            true,
            5
        );

        String result = query.toQueryString();

        assertTrue(result.contains("lat=49.2827"));
        assertTrue(result.contains("category=cafe"));
        assertTrue(result.contains("indoorPreferred=true"));
        assertTrue(result.contains("atmosphere=quiet+social"));
        assertTrue(result.contains("openOnly=true"));
        assertTrue(result.contains("limit=5"));
    }

    @Test
    public void queryOmitsPreferencesThatWereNotSelected() {
        RecommendationQuery query = new RecommendationQuery(
            49.2827,
            -123.1207,
            null,
            null,
            20,
            null,
            " ",
            false,
            10
        );

        String result = query.toQueryString();

        assertFalse(result.contains("category="));
        assertFalse(result.contains("indoorPreferred="));
        assertFalse(result.contains("budgetPreference="));
        assertFalse(result.contains("atmosphere="));
    }

    @Test
    public void foodUiCategoryUsesRestaurantApiCategory() {
        RecommendationQuery query = new RecommendationQuery(
            49.2827,
            -123.1207,
            "Food",
            null,
            20,
            null,
            null,
            false,
            10
        );

        assertTrue(query.toQueryString().contains("category=restaurant"));
        assertFalse(query.toQueryString().contains("category=food"));
    }
}
