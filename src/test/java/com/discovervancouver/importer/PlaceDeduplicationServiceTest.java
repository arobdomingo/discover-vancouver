package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.discovervancouver.model.Place;

class PlaceDeduplicationServiceTest {

    private final PlaceDeduplicationService service =
        new PlaceDeduplicationService(null);

    @Test
    void exactNormalizedNameIsHighConfidenceMatch() {
        assertTrue(service.isHighConfidenceMatch(
            place("Vancouver Public Library – Central", "350 W Georgia St"),
            place("Vancouver Public Library Central", "Different address")
        ));
    }

    @Test
    void nearbyButUnrelatedPlacesAreNotMerged() {
        assertFalse(service.isHighConfidenceMatch(
            place("Harbour Coffee", "100 Test St"),
            place("Downtown Pharmacy", "102 Test St")
        ));
    }

    private Place place(String name, String address) {
        return new Place(
            null, name, "attraction", "Test place.", address, 0, false,
            Map.of(), 49.28, -123.12, List.of()
        );
    }
}
