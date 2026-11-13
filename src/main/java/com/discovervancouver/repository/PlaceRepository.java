package com.discovervancouver.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.geo.Distance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.discovervancouver.model.Place;

public interface PlaceRepository extends MongoRepository<Place, String> {

    List<Place> findByCategoryIgnoreCase(String category);

    Page<Place> findByCategoryIgnoreCase(
        String category,
        Pageable pageable
    );

    List<Place> findByLocationNear(
        GeoJsonPoint location,
        Distance maximumDistance
    );

    Optional<Place> findBySourceAndSourceId(
        String source,
        String sourceId
    );

    List<Place> findAllBySource(String source);

    List<Place> findAllByCanonicalPlaceId(String canonicalPlaceId);
    
}
