package com.discovervancouver.dto;

import com.discovervancouver.model.FeedbackEventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
    @NotBlank
    @Size(max = 100)
    String placeId,

    @NotNull
    FeedbackEventType eventType
) {
}
