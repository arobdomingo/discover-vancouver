package com.discovervancouver.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.discovervancouver.dto.CreatePlaceRequest;
import com.discovervancouver.dto.PlaceResponse;
import com.discovervancouver.dto.PagedResponse;
import com.discovervancouver.dto.UpdatePlaceRequest;
import com.discovervancouver.mapper.PlaceMapper;
import com.discovervancouver.exception.ResourceNotFoundException;

import com.discovervancouver.model.Place;
import com.discovervancouver.service.PlaceService;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceMapper placeMapper;
    
    public PlaceController(PlaceService placeService, PlaceMapper placeMapper) {
        this.placeService = placeService;
        this.placeMapper = placeMapper;
    }

    @GetMapping
    public PagedResponse<PlaceResponse> getPlaces(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "page must be at least 0")
        int page,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        int size
    ) {
        return PagedResponse.from(
            placeService.getPlaces(category, page, size)
                .map(placeMapper::toResponse)
        );
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceResponse> getPlaceById(@PathVariable String placeId) {
        return placeService.getPlaceById(placeId)
            .map(placeMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Place not found: " + placeId
            ));
    }

    @PostMapping
    public ResponseEntity<PlaceResponse> createPlace(@Valid @RequestBody CreatePlaceRequest request, UriComponentsBuilder uriBuilder){

        Place place = placeMapper.toEntity(request);
        Place savedPlace = placeService.createPlace(place);

        URI location = uriBuilder
            .path("/api/v1/places/{placeId}")
            .buildAndExpand(savedPlace.getId())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(placeMapper.toResponse(savedPlace));
    }

    @PutMapping("/{placeId}")
    public ResponseEntity<PlaceResponse> updatePlace(@PathVariable String placeId, @Valid @RequestBody UpdatePlaceRequest request){
        Place updatedPlace = placeMapper.toEntity(placeId, request);

        return placeService.updatePlace(placeId, updatedPlace)
            .map(placeMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Place not found: " + placeId
            ));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> deletePlace(@PathVariable String placeId){
        boolean deleted = placeService.deletePlace(placeId);

        if(!deleted){
            throw new ResourceNotFoundException(
                "Place not found: " + placeId
            );
        }
        return ResponseEntity.noContent().build();
    }

}

