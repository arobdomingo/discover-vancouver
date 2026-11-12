package com.discovervancouver.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;

import com.discovervancouver.model.Place;
import com.discovervancouver.model.OpeningPeriod;

@DataMongoTest(properties = {
    "spring.data.mongodb.uri="
        + "mongodb://localhost:27017/discover_vancouver_test"
})
class PlaceRepositoryIntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void cleanBeforeTest() {
        placeRepository.deleteAll();
    }

    @AfterEach
    void cleanAfterTest() {
        placeRepository.deleteAll();
    }

    @Test
    void mongoGeneratesIdAndCategoryQueryIgnoresCase() {
        Place place = new Place(
            null,
            "Test Cafe",
            "cafe",
            "A place used by an integration test.",
            "123 Test St, Vancouver, BC",
            1,
            true,
            Map.of(
                DayOfWeek.MONDAY,
                List.of(new OpeningPeriod(
                    LocalTime.of(8, 0),
                    LocalTime.of(18, 0)
                ))
            ),
            49.2827,
            -123.1207,
            List.of("coffee", "quiet")
        );

        Place savedPlace = placeRepository.save(place);

        assertNotNull(savedPlace.getId());

        List<Place> results =
            placeRepository.findByCategoryIgnoreCase("CAFE");

        assertFalse(results.isEmpty());
        assertEquals(savedPlace.getId(), results.get(0).getId());
    }

    @Test
    void nearbyQueryUsesGeospatialDistance() {
        Place nearby = testPlace(
            "nearby",
            "Nearby Cafe",
            49.2827,
            -123.1207
        );
        Place farAway = testPlace(
            "far-away",
            "Far Away Cafe",
            49.3043,
            -123.1443
        );
        placeRepository.saveAll(List.of(nearby, farAway));

        List<Place> results = placeRepository.findByLocationNear(
            new org.springframework.data.mongodb.core.geo.GeoJsonPoint(
                -123.1207,
                49.2827
            ),
            new org.springframework.data.geo.Distance(
                1.0,
                org.springframework.data.geo.Metrics.KILOMETERS
            )
        );

        assertEquals(List.of("nearby"), results.stream()
            .map(Place::getId)
            .toList());
    }

    private Place testPlace(
        String id,
        String name,
        double latitude,
        double longitude
    ) {
        return new Place(
            id,
            name,
            "cafe",
            "A place used by a geospatial integration test.",
            "123 Test St, Vancouver, BC",
            1,
            true,
            Map.of(
                DayOfWeek.MONDAY,
                List.of(new OpeningPeriod(
                    LocalTime.of(8, 0),
                    LocalTime.of(18, 0)
                ))
            ),
            latitude,
            longitude,
            List.of("coffee")
        );
    }
}
