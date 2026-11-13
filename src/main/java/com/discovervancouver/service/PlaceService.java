package com.discovervancouver.service;

import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Map;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import com.discovervancouver.model.Place;
import com.discovervancouver.repository.PlaceRepository;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    
    public PlaceService(PlaceRepository placeRepository){
        this.placeRepository = placeRepository;
    }

    public List<Place> getPlaces(String category){
        if(category == null || category.isBlank()){
            return placeRepository.findAll();
        }
        return placeRepository.findByCategoryIgnoreCase(category);
    }

    public Page<Place> getPlaces(
        String category,
        int page,
        int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.ASC, "name")
        );
        if (category == null || category.isBlank()) {
            return placeRepository.findAll(pageable);
        }
        return placeRepository.findByCategoryIgnoreCase(
            category,
            pageable
        );
    }

    public List<Place> getPlacesNear(
        double latitude,
        double longitude,
        int maximumDistanceMeters
    ) {
        GeoJsonPoint origin = new GeoJsonPoint(longitude, latitude);
        Distance radius = new Distance(
            maximumDistanceMeters / 1_000.0,
            Metrics.KILOMETERS
        );

        Map<String, Place> canonicalPlaces = new LinkedHashMap<>();
        placeRepository.findByLocationNear(origin, radius).stream()
            .filter(Place::isDiscoverable)
            .forEach(place -> canonicalPlaces.merge(
                canonicalKey(place),
                place,
                this::preferHigherQuality
            ));
        return List.copyOf(canonicalPlaces.values());
    }
    
    public Optional<Place> getPlaceById(String placeId){
        return placeRepository.findById(placeId);
    }

    public Place createPlace(Place place){
        return placeRepository.save(place);
    }


    public boolean deletePlace(String placeId){
        if(!placeRepository.existsById(placeId)){
            return false;
        }

        placeRepository.deleteById(placeId);
        return true;
    }

    public Optional<Place> updatePlace(String placeId, Place updatedPlace){
        if(!placeRepository.existsById(placeId)){
            return Optional.empty();
        }

        updatedPlace.setId(placeId);
        return Optional.of(placeRepository.save(updatedPlace));
    }

    private String canonicalKey(Place place) {
        return place.getCanonicalPlaceId() == null
            || place.getCanonicalPlaceId().isBlank()
                ? "record:" + place.getId()
                : "canonical:" + place.getCanonicalPlaceId();
    }

    private Place preferHigherQuality(Place first, Place second) {
        Comparator<Place> comparator = Comparator
            .comparingInt((Place place) -> place.getDataQuality().ordinal())
            .thenComparingInt(place -> -completeness(place));
        return comparator.compare(first, second) <= 0 ? first : second;
    }

    private int completeness(Place place) {
        int score = 0;
        if (place.isHoursKnown()) score++;
        if (place.isPriceKnown()) score++;
        if (place.isIndoorKnown()) score++;
        if (place.getDescription() != null && !place.getDescription().isBlank()) score++;
        return score;
    }
}
