package com.discovervancouver.dto;

import java.util.Map;

import com.discovervancouver.model.FeedbackEventType;

public record FeedbackSummaryResponse(
    long totalEvents,
    Map<FeedbackEventType, Long> eventsByType
) {
}
