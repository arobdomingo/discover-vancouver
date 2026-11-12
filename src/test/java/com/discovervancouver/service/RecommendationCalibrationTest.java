package com.discovervancouver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.discovervancouver.config.RecommendationProperties;
import com.discovervancouver.dto.RecommendationResponse;
import com.discovervancouver.dto.WeatherResponse;
import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.model.Place;

class RecommendationCalibrationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("calibrationScenarios")
    void expectedPlaceRanksFirst(
        String scenarioName,
        List<Place> candidates,
        String category,
        boolean rainy,
        String expectedPlaceId
    ) {
        PlaceService placeService = mock(PlaceService.class);
        WeatherService weatherService = mock(WeatherService.class);
        TimeOfDayService timeOfDayService = mock(TimeOfDayService.class);
        when(placeService.getPlacesNear(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(candidates);
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
            .thenReturn(new WeatherResponse(
                10.0,
                rainy ? 1.0 : 0.0,
                rainy ? 61 : 3,
                rainy
            ));
        when(timeOfDayService.currentTag()).thenReturn("afternoon");

        RecommendationService service = new RecommendationService(
            placeService,
            new DistanceService(),
            weatherService,
            new OpeningHoursService(
                Clock.fixed(
                    Instant.parse("2026-11-12T20:00:00Z"),
                    ZoneId.of("UTC")
                ),
                ZoneId.of("America/Vancouver")
            ),
            timeOfDayService,
            new RecommendationProperties()
        );

        List<RecommendationResponse> results = service.getRecommendations(
            49.2827,
            -123.1207,
            category,
            null,
            20,
            null,
            null
        );

        assertEquals(expectedPlaceId, results.get(0).placeId());
    }

    private static Stream<Arguments> calibrationScenarios() {
        Place cafe = place(
            "cafe",
            "Cafe",
            "cafe",
            true,
            49.2827,
            -123.1207,
            List.of("quiet")
        );
        Place park = place(
            "park",
            "Park",
            "park",
            false,
            49.2827,
            -123.1207,
            List.of("outdoor")
        );
        Place rainyLibrary = place(
            "library",
            "Library",
            "study",
            true,
            49.2827,
            -123.1207,
            List.of("rainy-day", "indoor")
        );
        Place near = place(
            "near",
            "Near Cafe",
            "cafe",
            true,
            49.2827,
            -123.1207,
            List.of("quiet")
        );
        Place far = place(
            "far",
            "Far Cafe",
            "cafe",
            true,
            49.2910,
            -123.1207,
            List.of("quiet")
        );

        return Stream.of(
            Arguments.of(
                "category preference",
                List.of(park, cafe),
                "cafe",
                false,
                "cafe"
            ),
            Arguments.of(
                "rain rewards sheltered rainy-day option",
                List.of(park, rainyLibrary),
                null,
                true,
                "library"
            ),
            Arguments.of(
                "distance breaks otherwise equal match",
                List.of(far, near),
                "cafe",
                false,
                "near"
            )
        );
    }

    private static Place place(
        String id,
        String name,
        String category,
        boolean indoor,
        double latitude,
        double longitude,
        List<String> tags
    ) {
        List<OpeningPeriod> allDay = List.of(
            new OpeningPeriod(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)
        );
        return new Place(
            id,
            name,
            category,
            "Calibration fixture.",
            "123 Test St, Vancouver, BC",
            1,
            indoor,
            Map.of(
                DayOfWeek.MONDAY, allDay,
                DayOfWeek.TUESDAY, allDay,
                DayOfWeek.WEDNESDAY, allDay,
                DayOfWeek.THURSDAY, allDay,
                DayOfWeek.FRIDAY, allDay,
                DayOfWeek.SATURDAY, allDay,
                DayOfWeek.SUNDAY, allDay
            ),
            latitude,
            longitude,
            tags
        );
    }
}
