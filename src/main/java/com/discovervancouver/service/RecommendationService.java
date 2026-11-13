package com.discovervancouver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.discovervancouver.dto.RecommendationResponse;
import com.discovervancouver.model.Place;
import com.discovervancouver.exception.WeatherUnavailableException;
import com.discovervancouver.config.RecommendationProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

@Service
public class RecommendationService {
    
    private final PlaceService placeService;
    private final DistanceService distanceService;
    private final WeatherService weatherService;
    private final OpeningHoursService openingHoursService;
    private final TimeOfDayService timeOfDayService;
    private final RecommendationProperties properties;
    private final MeterRegistry meterRegistry;
    private final Counter recommendationRequests;
    private final DistributionSummary recommendationResults;
    private final Timer recommendationLatency;

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    public RecommendationService(PlaceService placeService, DistanceService distanceService, WeatherService weatherService, OpeningHoursService openingHoursService, TimeOfDayService timeOfDayService, RecommendationProperties properties, MeterRegistry meterRegistry) {
        this.placeService = placeService;
        this.distanceService = distanceService;
        this.weatherService = weatherService;
        this.openingHoursService = openingHoursService;
        this.timeOfDayService = timeOfDayService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.recommendationRequests = meterRegistry.counter(
            "discover.recommendations.requests"
        );
        this.recommendationResults = DistributionSummary.builder(
                "discover.recommendations.results"
            )
            .register(meterRegistry);
        this.recommendationLatency = Timer.builder(
                "discover.recommendations.latency"
            )
            .register(meterRegistry);
    }

    public RecommendationService(PlaceService placeService, DistanceService distanceService, WeatherService weatherService, OpeningHoursService openingHoursService, TimeOfDayService timeOfDayService, RecommendationProperties properties) {
        this(
            placeService,
            distanceService,
            weatherService,
            openingHoursService,
            timeOfDayService,
            properties,
            Metrics.globalRegistry
        );
    }

    public List<RecommendationResponse> getRecommendations(double latitude, double longitude, String category, Boolean indoorPreferred, int maxWalkingMinutes, String budgetPreference, String atmosphere) {
        return getRecommendations(
            latitude,
            longitude,
            category,
            indoorPreferred,
            maxWalkingMinutes,
            budgetPreference,
            atmosphere,
            false,
            properties.getDefaultLimit()
        );
    }

    public List<RecommendationResponse> getRecommendations(double latitude, double longitude, String category, Boolean indoorPreferred, int maxWalkingMinutes, String budgetPreference, String atmosphere, boolean openOnly, int limit) {
        recommendationRequests.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<RecommendationResponse> recommendations = calculateRecommendations(
                latitude,
                longitude,
                category,
                indoorPreferred,
                maxWalkingMinutes,
                budgetPreference,
                atmosphere,
                openOnly,
                limit
            );
            recommendationResults.record(recommendations.size());
            return recommendations;
        } finally {
            sample.stop(recommendationLatency);
        }
    }

    private List<RecommendationResponse> calculateRecommendations(double latitude, double longitude, String category, Boolean indoorPreferred, int maxWalkingMinutes, String budgetPreference, String atmosphere, boolean openOnly, int limit) {
        boolean rainyWeather = getRainyWeatherOrDefault(latitude, longitude);
        int resultLimit = Math.min(limit, properties.getMaximumLimit());

        int maximumDistanceMeters =
            distanceService.maximumWalkingDistanceMeters(
                maxWalkingMinutes
            );

        return placeService.getPlacesNear(
                latitude,
                longitude,
                maximumDistanceMeters
            ).stream()
            .map(place -> createCandidate(place, latitude, longitude))
            .filter(candidate -> candidate.walkingMinutes() <= maxWalkingMinutes)
            .filter(candidate -> !openOnly
                || !candidate.place().isHoursKnown()
                || candidate.openNow())
            .map(candidate -> scorePlace(candidate, category, indoorPreferred, maxWalkingMinutes, budgetPreference, atmosphere, rainyWeather))
            .sorted(
                Comparator.comparingInt(RecommendationResponse::score)
                    .reversed()
                    .thenComparingInt(RecommendationResponse::walkingMinutes)
                    .thenComparing(RecommendationResponse::name)
            )
            .limit(resultLimit)
            .toList();
    }

    private boolean getRainyWeatherOrDefault(double latitude, double longitude){
        try {
            return weatherService
                .getCurrentWeatherResponse(latitude, longitude)
                .rainy();
        } catch (WeatherUnavailableException exception) {
            logger.warn(
                "Weather is unavailable; using weather-neutral scoring"
            );

            return false;
        }
    }

    private PlaceCandidate createCandidate(
        Place place,
        double latitude,
        double longitude
    ) {
        int distanceMeters = distanceService.distanceMeters(
            latitude,
            longitude,
            place.getLatitude(),
            place.getLongitude()
        );

        return new PlaceCandidate(
            place,
            distanceMeters,
            distanceService.walkingMinutes(distanceMeters),
            openingHoursService.isOpenNow(place)
        );
    }

    private RecommendationResponse scorePlace(PlaceCandidate candidate, String category, Boolean indoorPreferred, int maxWalkingMinutes, String budgetPreference, String atmosphere, boolean rainyWeather) {
        Place place = candidate.place();
        RecommendationProperties.Weights weights = properties.getWeights();
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if(category != null && !category.isBlank() && place.getCategory().equalsIgnoreCase(category)){
            score += weights.getCategory();
            reasons.add("Matches your " + category + " preference");
        }

        if(indoorPreferred != null
            && place.isIndoorKnown()
            && place.isIndoor() == indoorPreferred) {
            score += weights.getIndoor();
            reasons.add(indoorPreferred
                ? "Works well as an indoor option"
                : "Works well as an outdoor option"
            );
        }

        if(candidate.openNow()){
            score += weights.getOpenNow();
            reasons.add("Open now");
        }

        if(place.isPriceKnown()
            && budgetPreference != null
            && !budgetPreference.isBlank()) {
            Integer preferredPriceLevel = priceLevelForBudget(budgetPreference);

            if(preferredPriceLevel != null && place.getPriceLevel() <= preferredPriceLevel){
                score += weights.getBudget();
                reasons.add("Fits your " + budgetPreference + " budget preference");
            }
        }

        if(atmosphere != null && !atmosphere.isBlank() && place.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(atmosphere))){
            score += weights.getAtmosphere();
            reasons.add("Matches your " + atmosphere +" atmosphere preference");
        }

        if(rainyWeather && place.isIndoorKnown() && place.isIndoor()){
            score += weights.getRainyIndoor();
            reasons.add("Good indoor option for rainy weather");
        }

        if(rainyWeather && place.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase("rainy-day"))){
            score += weights.getRainyDayTag();
            reasons.add("Tagged as a good rainy-day option");
        }

        String currentTimeTag = timeOfDayService.currentTag();
        if (place.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(currentTimeTag))) {
            score += weights.getTimeOfDay();
            reasons.add("Good match for the current time of day");
        }

        int distanceScore = Math.max(
            0,
            weights.getDistanceMaximum() - candidate.walkingMinutes()
        );
        score += distanceScore;

        reasons.add("About " + candidate.walkingMinutes() + " minutes away on foot");

        if(candidate.walkingMinutes() <= maxWalkingMinutes) {
            reasons.add("Within your " + maxWalkingMinutes + " minute walking range");
        }

        return new RecommendationResponse(
            place.getId(),
            place.getName(),
            place.getCategory(),
            place.getDescription(),
            place.getAddress(),
            place.getPriceLevel(),
            place.isIndoor(),
            candidate.openNow(),
            place.getTags(),
            score,
            candidate.distanceMeters(),
            candidate.walkingMinutes(),
            place.getLatitude(),
            place.getLongitude(),
            rainyWeather,
            reasons,
            place.isPriceKnown(),
            place.isIndoorKnown(),
            place.isHoursKnown(),
            place.getSource()
        );
    }

    private record PlaceCandidate(
        Place place,
        int distanceMeters,
        int walkingMinutes,
        boolean openNow
    ) {
    }

    private Integer priceLevelForBudget(String budgetPreference){
        return switch (budgetPreference.toLowerCase()){
            case "free" -> 0;
            case "low" -> 1;
            case "medium" -> 2;
            case "high" -> 3;
            default -> null;
        };
    }

}
