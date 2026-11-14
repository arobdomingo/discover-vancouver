package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FoursquareCategoryMapperTest {

    private final FoursquareCategoryMapper mapper =
        new FoursquareCategoryMapper();

    @Test
    void categoryBreadcrumbTagsFitThePlaceValidationLimit() {
        FoursquareCategoryMapper.MappedCategory mapped =
            mapper.map(List.of(
                "Business and Professional Services > Advertising Agencies > Specialized Marketing Services"
            ));

        assertTrue(mapped.tags().stream()
            .allMatch(tag -> tag.length() <= 50));
    }

    @Test
    void publicTransportationIsNotMistakenForAPub() {
        assertEquals("other", mapper.map(List.of(
            "Travel and Transportation > Transportation Service > Public Transportation > Bus Line"
        )).category());
    }

    @Test
    void publishersAndPublicServicesAreNotNightlife() {
        assertEquals("other", mapper.map(List.of(
            "Business and Professional Services > Publisher"
        )).category());
        assertEquals("other", mapper.map(List.of(
            "Community and Government > Public and Social Service"
        )).category());
    }

    @Test
    void distinguishesRealBarsFromBrowAndJuiceBars() {
        assertEquals("nightlife", mapper.map(List.of(
            "Dining and Drinking > Bar > Cocktail Bar"
        )).category());
        assertEquals("other", mapper.map(List.of(
            "Business and Professional Services > Health and Beauty Service > Brow Bar"
        )).category());
        assertEquals("cafe", mapper.map(List.of(
            "Dining and Drinking > Juice Bar"
        )).category());
    }

    @Test
    void distinguishesParksFromParkingAndGardenRetail() {
        assertEquals("park", mapper.map(List.of(
            "Landmarks and Outdoors > Park"
        )).category());
        assertEquals("other", mapper.map(List.of(
            "Travel and Transportation > Parking"
        )).category());
        assertEquals("shopping", mapper.map(List.of(
            "Retail > Garden Center"
        )).category());
    }

    @Test
    void groceryRetailIsShoppingRatherThanRestaurant() {
        assertEquals("shopping", mapper.map(List.of(
            "Retail > Food and Beverage Retail > Grocery Store"
        )).category());
    }
}
