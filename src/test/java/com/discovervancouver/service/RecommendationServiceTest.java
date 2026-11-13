package com.discovervancouver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.discovervancouver.exception.WeatherUnavailableException;

import com.discovervancouver.dto.WeatherResponse;
import com.discovervancouver.dto.RecommendationResponse;
import com.discovervancouver.model.Place;
import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.model.DataQuality;
import com.discovervancouver.model.PlaceOperatingStatus;
import com.discovervancouver.config.RecommendationProperties;

public class RecommendationServiceTest {

    private final OpeningHoursService openingHoursService =
        new OpeningHoursService(
            Clock.fixed(
                Instant.parse("2026-10-26T19:00:00Z"),
                ZoneId.of("UTC")
            ),
            ZoneId.of("America/Vancouver")
        );
    private final TimeOfDayService timeOfDayService =
        mock(TimeOfDayService.class);
    private final RecommendationProperties properties =
        new RecommendationProperties();

    @Test
    void matchingCategoryRanksHigher(){
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);


        Place cafe = new Place(
            "place1",
            "Breka Bakery & Cafe",
            "cafe",
            "Casual cafe with pastries and study-friendly seating.",
            "855 Davie St, Vancouver, BC",
            1,
            true,
            alwaysOpenHours(),
            49.2810,
            -123.1170,
            List.of("cafe", "study", "casual", "late-night", "wifi")
        );

        Place park = new Place(
            "place2",
            "Nelson Park",
            "park",
            "Small downtown green space for a calm outdoor reset.",
            "1030 Bute St, Vancouver, BC",
            0,
            false,
            alwaysOpenHours(),
            49.2813,
            -123.1308,
            List.of("park", "quiet", "walk", "free", "outdoor")
        );

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(cafe, park));
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
        .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));


        RecommendationService recommendationService = new RecommendationService(placeService, distanceService, weatherService, openingHoursService, timeOfDayService, properties);

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            "cafe",
            true,
            20,
            "low",
            null
        );

        assertEquals("place1", recommendations.get(0).placeId());
    }

    @Test
    void placesOusideWalkingRangeFilteredOut(){
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);

        Place nearbyCafe = new Place(
            "place1",
            "Breka Bakery & Cafe",
            "cafe",
            "Casual cafe with pastries and study-friendly seating.",
            "855 Davie St, Vancouver, BC",
            1,
            true,
            alwaysOpenHours(),
            49.2810,
            -123.1170,
            List.of("cafe", "study", "casual", "late-night", "wifi")
        );

        Place farPark = new Place(
            "place2",
            "Stanley Park",
            "park",
            "Large urban park with trails and waterfront views.",
            "Stanley Park Dr, Vancouver, BC",
            0,
            false,
            alwaysOpenHours(),
            49.3043,
            -123.1443,
            List.of("park", "walk", "outdoor", "scenic")
        );

        when(placeService.getPlacesNear(49.2827, -123.1207, 800))
            .thenReturn(List.of(nearbyCafe, farPark));
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
        .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            distanceService,
            weatherService,
            openingHoursService,
            timeOfDayService,
            properties
        );

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            null,
            null,
            10,
            null,
            null
        );

        assertEquals(1, recommendations.size());
        assertEquals("place1", recommendations.get(0).placeId());
    }

    @Test
    void matchingAtmosphereAddsReason(){
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);


        Place library = new Place(
            "place1",
            "Vancouver Public Library Central",
            "study",
            "Iconic central library with quiet indoor work areas.",
            "350 W Georgia St, Vancouver, BC",
            0,
            true,
            alwaysOpenHours(),
            49.2797,
            -123.1156,
            List.of("study", "quiet", "free", "rainy-day", "indoor")
        );

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(library));
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
        .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
                placeService,
                distanceService,
                weatherService,
                openingHoursService,
                timeOfDayService,
                properties
        );

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            null,
            null,
            20,
            null,
            "quiet"
        );

        assertEquals(1, recommendations.size());
        assertEquals("place1", recommendations.get(0).placeId());
        assertEquals(true, recommendations.get(0).reasons().contains("Matches your quiet atmosphere preference"));

    }

    @Test
    void matchingBudgetAddsReason() {
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);
    
        Place library = new Place(
                "place1",
                "Vancouver Public Library Central",
                "study",
                "Iconic central library with quiet indoor work areas.",
                "350 W Georgia St, Vancouver, BC",
                0,
                true,
                alwaysOpenHours(),
                49.2797,
                -123.1156,
                List.of("study", "quiet", "free", "rainy-day", "indoor")
        );
    
        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(library));
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
        .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            distanceService,
            weatherService,
            openingHoursService,
            timeOfDayService,
            properties
        );

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            null,
            null,
            20,
            "free",
            null
        );

        assertEquals(1, recommendations.size());
        assertTrue(recommendations.get(0).reasons().contains("Fits your free budget preference"));
    
    }

    @Test
    void rainyWeatherBoostsIndoorRainyDayPlaces(){
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);

        Place library = new Place(
            "place1",
            "Vancouver Public Library Central",
            "study",
            "Iconic central library with quiet indoor work areas.",
            "350 W Georgia St, Vancouver, BC",
            0,
            true,
            alwaysOpenHours(),
            49.2797,
            -123.1156,
            List.of("study", "quiet", "free", "rainy-day", "indoor")
        );


        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
        .thenReturn(new WeatherResponse(9.0, 1.2, 61, true));
        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(library));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            distanceService,
            weatherService,
            openingHoursService,
            timeOfDayService,
            properties
        );
        
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            null,
            null,
            20,
            null,
            null
        );

        assertEquals(1, recommendations.size());
        assertTrue(recommendations.get(0).rainyWeather());
        assertTrue(recommendations.get(0).reasons().contains("Good indoor option for rainy weather"));
        assertTrue(recommendations.get(0).reasons().contains("Tagged as a good rainy-day option"));
    }

        @Test
    void recommendationsStillWorkWhenWeatherIsUnavailable() {
        PlaceService placeService = mock(PlaceService.class);
        DistanceService distanceService = new DistanceService();
        WeatherService weatherService = mock(WeatherService.class);

        Place library = new Place(
            "place1",
            "Vancouver Public Library Central",
            "study",
            "Quiet indoor work areas.",
            "350 W Georgia St, Vancouver, BC",
            0,
            true,
            alwaysOpenHours(),
            49.2797,
            -123.1156,
            List.of("study", "quiet", "indoor")
        );

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(library));

        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207)).thenThrow(
            new WeatherUnavailableException(
                "Current weather is temporarily unavailable",
                new RuntimeException("Simulated failure")
            )
        );

        RecommendationService recommendationService = new RecommendationService(placeService, distanceService, weatherService, openingHoursService, timeOfDayService, properties);

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(49.2827, -123.1207, null, null, 20, null, null);

        assertEquals(1, recommendations.size());
        assertEquals("place1", recommendations.get(0).placeId());
        assertEquals(false, recommendations.get(0).rainyWeather());
    }

    @Test
    void openOnlyRemovesClosedPlacesBeforeScoring() {
        PlaceService placeService = mock(PlaceService.class);
        WeatherService weatherService = mock(WeatherService.class);
        OpeningHoursService hoursService = mock(OpeningHoursService.class);
        Place openPlace = testPlace("open", "Open Place", List.of("cafe"));
        Place closedPlace = testPlace("closed", "Closed Place", List.of("cafe"));

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(closedPlace, openPlace));
        when(hoursService.isOpenNow(openPlace)).thenReturn(true);
        when(hoursService.isOpenNow(closedPlace)).thenReturn(false);
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
            .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            new DistanceService(),
            weatherService,
            hoursService,
            timeOfDayService,
            properties
        );

        List<RecommendationResponse> recommendations =
            recommendationService.getRecommendations(
                49.2827,
                -123.1207,
                null,
                null,
                20,
                null,
                null,
                true,
                10
            );

        assertEquals(1, recommendations.size());
        assertEquals("open", recommendations.get(0).placeId());
    }

    @Test
    void unknownFactsRemainEligibleWithoutReceivingMatchBonuses() {
        PlaceService placeService = mock(PlaceService.class);
        WeatherService weatherService = mock(WeatherService.class);
        OpeningHoursService hoursService = mock(OpeningHoursService.class);
        Place unknown = new Place(
            "unknown",
            "Unverified Cafe",
            "cafe",
            "A source record with incomplete attributes.",
            "Vancouver, BC",
            0,
            false,
            Map.of(),
            49.2827,
            -123.1207,
            List.of("cafe")
        );
        unknown.setImportMetadata(
            "foursquare-open-places",
            "unknown-1",
            null,
            Instant.parse("2026-10-01T00:00:00Z"),
            null,
            Instant.parse("2026-10-01T00:00:00Z"),
            PlaceOperatingStatus.OPEN,
            DataQuality.MEDIUM,
            false,
            false,
            false,
            Map.of()
        );

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(unknown));
        when(hoursService.isOpenNow(unknown)).thenReturn(false);
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
            .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            new DistanceService(),
            weatherService,
            hoursService,
            timeOfDayService,
            properties
        );

        RecommendationResponse result = recommendationService.getRecommendations(
            49.2827,
            -123.1207,
            null,
            true,
            20,
            "free",
            null,
            true,
            10
        ).get(0);

        assertEquals(false, result.priceKnown());
        assertEquals(false, result.indoorKnown());
        assertEquals(false, result.hoursKnown());
        assertEquals(false, result.reasons().stream()
            .anyMatch(reason -> reason.contains("budget preference")));
        assertEquals(false, result.reasons().stream()
            .anyMatch(reason -> reason.contains("indoor option")));
    }

    @Test
    void timeOfDayBoostAndLimitAreApplied() {
        PlaceService placeService = mock(PlaceService.class);
        WeatherService weatherService = mock(WeatherService.class);
        TimeOfDayService morningTime = mock(TimeOfDayService.class);
        Place generalCafe = testPlace("general", "A General Cafe", List.of("cafe"));
        Place morningCafe = testPlace("morning", "Z Morning Cafe", List.of("cafe", "morning"));

        when(placeService.getPlacesNear(49.2827, -123.1207, 1_600))
            .thenReturn(List.of(generalCafe, morningCafe));
        when(morningTime.currentTag()).thenReturn("morning");
        when(weatherService.getCurrentWeatherResponse(49.2827, -123.1207))
            .thenReturn(new WeatherResponse(12.0, 0.0, 3, false));

        RecommendationService recommendationService = new RecommendationService(
            placeService,
            new DistanceService(),
            weatherService,
            openingHoursService,
            morningTime,
            properties
        );

        List<RecommendationResponse> recommendations =
            recommendationService.getRecommendations(
                49.2827,
                -123.1207,
                null,
                null,
                20,
                null,
                null,
                false,
                1
            );

        assertEquals(1, recommendations.size());
        assertEquals("morning", recommendations.get(0).placeId());
        assertTrue(recommendations.get(0).reasons()
            .contains("Good match for the current time of day"));
    }

    private Place testPlace(String id, String name, List<String> tags) {
        return new Place(
            id,
            name,
            "cafe",
            "A test cafe.",
            "123 Test St, Vancouver, BC",
            1,
            true,
            alwaysOpenHours(),
            49.2827,
            -123.1207,
            tags
        );
    }

    private Map<DayOfWeek, List<OpeningPeriod>> alwaysOpenHours() {
        List<OpeningPeriod> allDay = List.of(
            new OpeningPeriod(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)
        );

        return Map.of(
            DayOfWeek.MONDAY, allDay,
            DayOfWeek.TUESDAY, allDay,
            DayOfWeek.WEDNESDAY, allDay,
            DayOfWeek.THURSDAY, allDay,
            DayOfWeek.FRIDAY, allDay,
            DayOfWeek.SATURDAY, allDay,
            DayOfWeek.SUNDAY, allDay
        );
    }

}
