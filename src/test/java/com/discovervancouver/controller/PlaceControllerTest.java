package com.discovervancouver.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.discovervancouver.mapper.PlaceMapper;
import com.discovervancouver.model.Place;
import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.service.OpeningHoursService;
import com.discovervancouver.service.PlaceService;
import com.discovervancouver.web.RateLimitFilter;
import com.discovervancouver.web.RequestIdFilter;

@WebMvcTest(
    controllers = PlaceController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {RateLimitFilter.class, RequestIdFilter.class}
    )
)
@Import(PlaceMapper.class)
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private OpeningHoursService openingHoursService;

    @Test
    void getPlacesReturnsMappedPlaces() throws Exception {
        when(openingHoursService.isOpenNow(any(Place.class)))
            .thenReturn(true);
        when(placeService.getPlaces(null, 0, 20))
            .thenReturn(new PageImpl<>(
                List.of(createLibrary("place1")),
                PageRequest.of(0, 20),
                1
            ));

        mockMvc.perform(get("/api/v1/places"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("place1"))
            .andExpect(jsonPath("$.content[0].name")
                .value("Vancouver Public Library Central"))
            .andExpect(jsonPath("$.content[0].category").value("study"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createPlaceReturnsCreatedResource() throws Exception {
        when(openingHoursService.isOpenNow(any(Place.class)))
            .thenReturn(true);
        when(placeService.createPlace(any(Place.class)))
            .thenAnswer(invocation -> {
                Place place = invocation.getArgument(0);
                place.setId("generated-id");
                return place;
            });

        String requestBody = """
            {
              "name": "Vancouver Art Gallery",
              "category": "museum",
              "description": "Downtown art gallery.",
              "address": "750 Hornby St, Vancouver, BC",
              "priceLevel": 2,
              "indoor": true,
              "openingHours": {
                "MONDAY": [{"opensAt": "09:00", "closesAt": "17:00"}]
              },
              "latitude": 49.2829,
              "longitude": -123.1205,
              "tags": ["art", "indoor"]
            }
            """;

        mockMvc.perform(post("/api/v1/places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value("generated-id"))
            .andExpect(jsonPath("$.name")
                .value("Vancouver Art Gallery"));
    }

    @Test
    void invalidPlaceReturnsStructuredBadRequest() throws Exception {
        String requestBody = """
            {
              "name": "",
              "category": "",
              "description": "",
              "address": "",
              "priceLevel": 9,
              "indoor": true,
              "openingHours": {},
              "latitude": 200,
              "longitude": -300,
              "tags": [""]
            }
            """;

        mockMvc.perform(post("/api/v1/places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Request body validation failed"))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.priceLevel").exists());
    }

    private Place createLibrary(String id) {
        return new Place(
            id,
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
    }

    private Map<DayOfWeek, List<OpeningPeriod>> alwaysOpenHours() {
        return Map.of(
            DayOfWeek.MONDAY,
            List.of(new OpeningPeriod(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT))
        );
    }
}
