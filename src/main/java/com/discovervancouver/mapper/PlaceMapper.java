package com.discovervancouver.mapper;

import org.springframework.stereotype.Component;

import com.discovervancouver.model.Place;
import com.discovervancouver.dto.CreatePlaceRequest;
import com.discovervancouver.dto.UpdatePlaceRequest;
import com.discovervancouver.dto.PlaceResponse;
import com.discovervancouver.service.OpeningHoursService;

@Component
public class PlaceMapper{

    private final OpeningHoursService openingHoursService;

    public PlaceMapper(OpeningHoursService openingHoursService) {
        this.openingHoursService = openingHoursService;
    }

    public Place toEntity(CreatePlaceRequest request){
        return new Place(
            null,
            request.name(),
            request.category(),
            request.description(),
            request.address(),
            request.priceLevel(),
            request.indoor(),
            request.openingHours(),
            request.latitude(),
            request.longitude(),
            request.tags()
        );
    }

    public Place toEntity(String placeId, UpdatePlaceRequest request){
        return new Place(
            placeId,
            request.name(),
            request.category(),
            request.description(),
            request.address(),
            request.priceLevel(),
            request.indoor(),
            request.openingHours(),
            request.latitude(),
            request.longitude(),
            request.tags()
        );
    }

    public PlaceResponse toResponse(Place place){
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getCategory(),
            place.getDescription(),
            place.getAddress(),
            place.getPriceLevel(),
            place.isIndoor(),
            openingHoursService.isOpenNow(place),
            place.getOpeningHours(),
            place.getLatitude(),
            place.getLongitude(),
            place.getTags(),
            place.isPriceKnown(),
            place.isIndoorKnown(),
            place.isHoursKnown(),
            place.getSource()
        );
    }

}
