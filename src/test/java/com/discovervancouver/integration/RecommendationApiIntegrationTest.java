package com.discovervancouver.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.discovervancouver.dto.WeatherResponse;
import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.model.Place;
import com.discovervancouver.repository.PlaceRepository;
import com.discovervancouver.service.WeatherService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlaceRepository placeRepository;

    @MockitoBean
    private WeatherService weatherService;

    @BeforeEach
    void loadKnownPlaces() {
        placeRepository.deleteAll();
        placeRepository.saveAll(List.of(
            place("near", "Near Test Cafe", 49.2828, -123.1207),
            place("far", "Far Test Cafe", 49.2920, -123.1207)
        ));
        when(weatherService.getCurrentWeatherResponse(
            49.2827,
            -123.1207
        )).thenReturn(new WeatherResponse(12, 0, 1, false));
    }

    @AfterEach
    void cleanDatabase() {
        placeRepository.deleteAll();
    }

    @Test
    void returnsKnownPlacesInScoreOrderWithExplanations() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")
                .header("X-Request-ID", "integration-request")
                .param("lat", "49.2827")
                .param("lng", "-123.1207")
                .param("category", "contract-test")
                .param("maxWalkingMinutes", "120")
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "X-Request-ID",
                "integration-request"
            ))
            .andExpect(jsonPath("$[0].name").value("Near Test Cafe"))
            .andExpect(jsonPath("$[1].name").value("Far Test Cafe"))
            .andExpect(jsonPath("$[0].reasons[0]")
                .value("Matches your contract-test preference"))
            .andExpect(jsonPath("$[0].reasons").isArray());
    }

    private Place place(
        String id,
        String name,
        double latitude,
        double longitude
    ) {
        List<OpeningPeriod> allDay = List.of(
            new OpeningPeriod(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)
        );
        return new Place(
            id,
            name,
            "contract-test",
            "Known integration-test fixture",
            "Vancouver, BC",
            1,
            true,
            Map.of(
                DayOfWeek.MONDAY, allDay,
                DayOfWeek.TUESDAY, allDay,
                DayOfWeek.WEDNESDAY, allDay,
                DayOfWeek.THURSDAY, allDay,
                DayOfWeek.FRIDAY, allDay,
                DayOfWeek.SATURDAY, allDay,
                DayOfWeek.SUNDAY, allDay
            ),
            latitude,
            longitude,
            List.of("contract-test")
        );
    }
}
