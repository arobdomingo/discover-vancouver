package com.discovervancouver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.discovervancouver.service.RecommendationService;
import com.discovervancouver.web.RateLimitFilter;
import com.discovervancouver.web.RequestIdFilter;

@WebMvcTest(
    controllers = RecommendationController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {RateLimitFilter.class, RequestIdFilter.class}
    )
)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void invalidRecommendationParametersReturnBadRequest()
        throws Exception {

        mockMvc.perform(get("/api/v1/recommendations")
                .param("lat", "200")
                .param("lng", "-300")
                .param("maxWalkingMinutes", "0")
                .param("budgetPreference", "impossible"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Request parameter validation failed"))
            .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void invalidParameterTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")
                .param("lat", "not-a-number")
                .param("lng", "-123.1207"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("A request parameter has the wrong type"))
            .andExpect(jsonPath("$.errors.lat").exists());
    }
}
