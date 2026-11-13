package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.discovervancouver.model.DataQuality;

class VancouverOpenDataMapperTest {

    private final VancouverOpenDataMapper mapper =
        new VancouverOpenDataMapper();

    @Test
    void mapsOfficialParkWithFacilitiesAndCoordinates() {
        Map<String, Object> raw = Map.of(
            "parkid", 14,
            "name", "Coopers' Park",
            "streetnumber", "1020",
            "streetname", "Marinaside Crescent",
            "neighbourhoodname", "Downtown",
            "googlemapdest", Map.of(
                "lat", 49.273253,
                "lon", -123.1139
            )
        );

        PlaceImportRecord record = mapper.map(
            VancouverDataset.PARKS,
            raw,
            List.of("Playground", "Public Washroom")
        ).orElseThrow();

        assertEquals("14", record.sourceId());
        assertEquals("park", record.category());
        assertEquals(49.273253, record.latitude());
        assertTrue(record.tags().contains("playground"));
        assertTrue(record.priceKnown());
        assertEquals(DataQuality.VERIFIED, record.dataQuality());
    }

    @Test
    void rejectsRecordWithoutCoordinates() {
        assertTrue(mapper.map(
            VancouverDataset.LIBRARIES,
            Map.of("name", "Unknown Library"),
            List.of()
        ).isEmpty());
    }

    @Test
    void mapsCurrentCulturalSpaceFieldNamesAndNestedGeometry() {
        Map<String, Object> raw = Map.of(
            "cultural_space_name", "Acme Studios",
            "address", "112 E Hastings St, Vancouver, BC",
            "type", "Studio/Rehearsal",
            "primary_use", "Artist Studio",
            "geom", Map.of(
                "type", "Feature",
                "geometry", Map.of(
                    "type", "Point",
                    "coordinates", List.of(-123.101791, 49.281101)
                )
            )
        );

        PlaceImportRecord record = mapper.map(
            VancouverDataset.CULTURAL_SPACES,
            raw,
            List.of()
        ).orElseThrow();

        assertEquals("Acme Studios", record.name());
        assertEquals(49.281101, record.latitude());
        assertEquals(-123.101791, record.longitude());
    }

    @Test
    void mapsFoodVendorWhenBusinessNameIsMissing() {
        Map<String, Object> raw = new java.util.HashMap<>();
        raw.put("key", "DT21");
        raw.put("business_name", null);
        raw.put("description", "Hot Dogs");
        raw.put("location", "South Side of 300 Davie St");
        raw.put("geo_point_2d", Map.of(
            "lat", 49.2747502296,
            "lon", -123.122779848
        ));

        PlaceImportRecord record = mapper.map(
            VancouverDataset.FOOD_VENDORS,
            raw,
            List.of()
        ).orElseThrow();

        assertEquals("DT21", record.sourceId());
        assertEquals("Hot Dogs", record.name());
        assertEquals("South Side of 300 Davie St", record.address());
    }
}
