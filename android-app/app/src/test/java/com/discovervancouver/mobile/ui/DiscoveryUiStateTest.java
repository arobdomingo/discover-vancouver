package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.discovervancouver.mobile.model.Recommendation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryUiStateTest {

    @Test
    public void successCopiesRecommendationList() {
        List<Recommendation> source = new ArrayList<>();
        source.add(recommendation("place1"));

        DiscoveryUiState state = DiscoveryUiState.success(source);
        source.clear();

        assertEquals(1, state.recommendations().size());
        assertEquals(DiscoveryUiState.Status.SUCCESS, state.status());
        assertThrows(
            UnsupportedOperationException.class,
            () -> state.recommendations().clear()
        );
    }

    @Test
    public void errorStoresMessageWithoutResults() {
        DiscoveryUiState state = DiscoveryUiState.error("offline");

        assertEquals(DiscoveryUiState.Status.ERROR, state.status());
        assertEquals("offline", state.errorMessage());
        assertEquals(List.of(), state.recommendations());
    }

    private Recommendation recommendation(String placeId) {
        return new Recommendation(
            placeId,
            "Test place",
            "cafe",
            "Description",
            "Address",
            1,
            true,
            true,
            List.of("quiet"),
            80,
            500,
            7,
            49.2827,
            -123.1207,
            false,
            List.of("Nearby")
        );
    }
}
