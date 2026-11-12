package com.discovervancouver.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.discovervancouver.dto.CreateFeedbackRequest;
import com.discovervancouver.dto.FeedbackSummaryResponse;
import com.discovervancouver.model.FeedbackEventType;
import com.discovervancouver.service.FeedbackService;

import static org.mockito.Mockito.when;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void acceptsAnonymousProductFeedback() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "placeId": "place1",
                      "eventType": "FAVORITED"
                    }
                    """))
            .andExpect(status().isAccepted());

        verify(feedbackService).record(new CreateFeedbackRequest(
            "place1",
            FeedbackEventType.FAVORITED
        ));
    }

    @Test
    void returnsAggregateFeedbackSummary() throws Exception {
        when(feedbackService.summarize()).thenReturn(
            new FeedbackSummaryResponse(
                3,
                Map.of(FeedbackEventType.FAVORITED, 3L)
            )
        );

        mockMvc.perform(get("/api/feedback/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").value(3))
            .andExpect(jsonPath("$.eventsByType.FAVORITED").value(3));
    }
}
