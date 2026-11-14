package com.discovervancouver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.discovervancouver.config.RecommendationProperties;
import com.discovervancouver.dto.RecommendationResponse;
import com.discovervancouver.exception.WeatherUnavailableException;
import com.discovervancouver.model.Place;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

@Service
public class RecommendationService {
    private static final Logger logger =
        LoggerFactory.getLogger(RecommendationService.class);

    private final PlaceService placeService;
    private final DistanceService distanceService;
    private final WeatherService weatherService;
    private final OpeningHoursService openingHoursService;
    private final TimeOfDayService timeOfDayService;
    private final RecommendationProperties properties;
    private final RecommendationPolicy policy;
    private final AtmosphereMatcher atmosphereMatcher;
    private final MeterRegistry meterRegistry;
    private final Counter recommendationRequests;
    private final DistributionSummary recommendationResults;
    private final Timer recommendationLatency;

    @Autowired
    public RecommendationService(
        PlaceService placeService,
        DistanceService distanceService,
        WeatherService weatherService,
        OpeningHoursService openingHoursService,
        TimeOfDayService timeOfDayService,
        RecommendationProperties properties,
        MeterRegistry meterRegistry,
        RecommendationPolicy policy,
        AtmosphereMatcher atmosphereMatcher
    ) {
        this.placeService = placeService;
        this.distanceService = distanceService;
        this.weatherService = weatherService;
        this.openingHoursService = openingHoursService;
        this.timeOfDayService = timeOfDayService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.policy = policy;
        this.atmosphereMatcher = atmosphereMatcher;
        recommendationRequests = meterRegistry.counter(
            "discover.recommendations.requests"
        );
        recommendationResults = DistributionSummary.builder(
            "discover.recommendations.results"
        ).register(meterRegistry);
        recommendationLatency = Timer.builder(
            "discover.recommendations.latency"
        ).register(meterRegistry);
    }

    public RecommendationService(
        PlaceService placeService,
        DistanceService distanceService,
        WeatherService weatherService,
        OpeningHoursService openingHoursService,
        TimeOfDayService timeOfDayService,
        RecommendationProperties properties
    ) {
        this(
            placeService,
            distanceService,
            weatherService,
            openingHoursService,
            timeOfDayService,
            properties,
            Metrics.globalRegistry,
            new RecommendationPolicy(),
            new AtmosphereMatcher()
        );
    }

    public List<RecommendationResponse> getRecommendations(
        double latitude,
        double longitude,
        String category,
        Boolean indoorPreferred,
        int maxWalkingMinutes,
        String budgetPreference,
        String atmosphere
    ) {
        return getRecommendations(
            latitude, longitude, category, indoorPreferred,
            maxWalkingMinutes, budgetPreference, atmosphere,
            false, properties.getDefaultLimit()
        );
    }

    public List<RecommendationResponse> getRecommendations(
        double latitude,
        double longitude,
        String category,
        Boolean indoorPreferred,
        int maxWalkingMinutes,
        String budgetPreference,
        String atmosphere,
        boolean openOnly,
        int limit
    ) {
        recommendationRequests.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<RecommendationResponse> recommendations = calculateRecommendations(
                latitude, longitude, category, indoorPreferred,
                maxWalkingMinutes, budgetPreference, atmosphere,
                openOnly, limit
            );
            recommendationResults.record(recommendations.size());
            return recommendations;
        } finally {
            sample.stop(recommendationLatency);
        }
    }

    private List<RecommendationResponse> calculateRecommendations(
        double latitude,
        double longitude,
        String category,
        Boolean indoorPreferred,
        int maxWalkingMinutes,
        String budgetPreference,
        String atmosphere,
        boolean openOnly,
        int limit
    ) {
        boolean rainyWeather = getRainyWeatherOrDefault(latitude, longitude);
        int resultLimit = Math.min(limit, properties.getMaximumLimit());
        int maximumDistanceMeters = distanceService.maximumWalkingDistanceMeters(
            maxWalkingMinutes
        );
        String normalizedCategory = policy.normalizeCategory(category);

        List<RankedRecommendation> ranked = placeService.getPlacesNear(
                latitude, longitude, maximumDistanceMeters
            ).stream()
            .map(place -> createCandidate(place, latitude, longitude))
            .filter(candidate ->
                candidate.walkingMinutes() <= maxWalkingMinutes)
            .filter(candidate -> !openOnly
                || candidate.place().isHoursKnown() && candidate.openNow())
            .filter(candidate -> policy.categoryMatches(
                candidate.place(), normalizedCategory
            ))
            .filter(candidate -> policy.budgetCompatible(
                candidate.place(), budgetPreference
            ))
            .map(candidate -> new RankedRecommendation(
                scorePlace(
                    candidate,
                    normalizedCategory,
                    indoorPreferred,
                    maxWalkingMinutes,
                    budgetPreference,
                    atmosphere,
                    rainyWeather
                ),
                policy.budgetPriority(candidate.place(), budgetPreference),
                atmosphereMatcher.matches(candidate.place(), atmosphere) ? 1 : 0
            ))
            .sorted(rankingComparator())
            .toList();

        return diversifyByName(ranked, resultLimit);
    }

    private Comparator<RankedRecommendation> rankingComparator() {
        return Comparator
            .comparingInt(RankedRecommendation::budgetPriority)
            .reversed()
            .thenComparing(
                Comparator.comparingInt(
                    RankedRecommendation::atmospherePriority
                ).reversed()
            )
            .thenComparing(
                Comparator.comparingInt(
                    (RankedRecommendation value) -> value.response().score()
                ).reversed()
            )
            .thenComparingInt(value -> value.response().walkingMinutes())
            .thenComparing(value -> value.response().name());
    }

    private boolean getRainyWeatherOrDefault(double latitude, double longitude) {
        try {
            return weatherService.getCurrentWeatherResponse(
                latitude, longitude
            ).rainy();
        } catch (WeatherUnavailableException exception) {
            logger.warn("Weather is unavailable; using weather-neutral scoring");
            return false;
        }
    }

    private PlaceCandidate createCandidate(
        Place place,
        double latitude,
        double longitude
    ) {
        int distanceMeters = distanceService.distanceMeters(
            latitude, longitude, place.getLatitude(), place.getLongitude()
        );
        return new PlaceCandidate(
            place,
            distanceMeters,
            distanceService.walkingMinutes(distanceMeters),
            openingHoursService.isOpenNow(place)
        );
    }

    private RecommendationResponse scorePlace(
        PlaceCandidate candidate,
        String category,
        Boolean indoorPreferred,
        int maxWalkingMinutes,
        String budgetPreference,
        String atmosphere,
        boolean rainyWeather
    ) {
        Place place = candidate.place();
        RecommendationProperties.Weights weights = properties.getWeights();
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (category != null && policy.categoryMatches(place, category)) {
            score += weights.getCategory();
            reasons.add("Matches your " + category + " preference");
        }
        if (indoorPreferred != null && place.isIndoorKnown()
            && place.isIndoor() == indoorPreferred) {
            score += weights.getIndoor();
            reasons.add(indoorPreferred
                ? "Works well as an indoor option"
                : "Works well as an outdoor option");
        }
        if (candidate.openNow()) {
            score += weights.getOpenNow();
            reasons.add("Open now");
        }
        Integer preferredPriceLevel = policy.maximumPrice(budgetPreference);
        if (place.isPriceKnown() && preferredPriceLevel != null
            && place.getPriceLevel() <= preferredPriceLevel) {
            score += weights.getBudget();
            reasons.add("Fits your " + budgetPreference
                + " budget preference");
        }
        if (atmosphereMatcher.matches(place, atmosphere)) {
            score += weights.getAtmosphere();
            reasons.add("Matches your " + atmosphere
                + " atmosphere preference");
        }
        if (rainyWeather && place.isIndoorKnown() && place.isIndoor()) {
            score += weights.getRainyIndoor();
            reasons.add("Good indoor option for rainy weather");
        }
        if (rainyWeather && hasTag(place, "rainy-day")) {
            score += weights.getRainyDayTag();
            reasons.add("Tagged as a good rainy-day option");
        }
        String currentTimeTag = timeOfDayService.currentTag();
        if (hasTag(place, currentTimeTag)) {
            score += weights.getTimeOfDay();
            reasons.add("Good match for the current time of day");
        }
        score += Math.max(
            0,
            weights.getDistanceMaximum() - candidate.walkingMinutes()
        );
        score += policy.dataQualityScore(place, weights.getDataQuality());
        score += policy.completenessScore(place, weights.getCompleteness());

        reasons.add("About " + candidate.walkingMinutes()
            + " minutes away on foot");
        reasons.add("Within your " + maxWalkingMinutes
            + " minute walking range");

        return new RecommendationResponse(
            place.getId(), place.getName(), place.getCategory(),
            place.getDescription(), place.getAddress(), place.getPriceLevel(),
            place.isIndoor(), candidate.openNow(), place.getTags(), score,
            candidate.distanceMeters(), candidate.walkingMinutes(),
            place.getLatitude(), place.getLongitude(), rainyWeather, reasons,
            place.isPriceKnown(), place.isIndoorKnown(), place.isHoursKnown(),
            place.getSource()
        );
    }

    private boolean hasTag(Place place, String tag) {
        return tag != null && place.getTags().stream()
            .anyMatch(value -> value.equalsIgnoreCase(tag));
    }

    private List<RecommendationResponse> diversifyByName(
        List<RankedRecommendation> ranked,
        int limit
    ) {
        Map<String, RecommendationResponse> unique = new LinkedHashMap<>();
        List<RecommendationResponse> repeated = new ArrayList<>();
        for (RankedRecommendation candidate : ranked) {
            RecommendationResponse response = candidate.response();
            String key = response.name().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
            if (unique.putIfAbsent(key, response) != null) {
                repeated.add(response);
            }
        }
        List<RecommendationResponse> results = new ArrayList<>(unique.values());
        results.addAll(repeated);
        return results.stream().limit(limit).toList();
    }

    private record PlaceCandidate(
        Place place,
        int distanceMeters,
        int walkingMinutes,
        boolean openNow
    ) { }

    private record RankedRecommendation(
        RecommendationResponse response,
        int budgetPriority,
        int atmospherePriority
    ) { }
}
