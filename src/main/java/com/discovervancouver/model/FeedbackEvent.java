package com.discovervancouver.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "feedback_events")
public class FeedbackEvent {

    @Id
    private String id;

    @Indexed
    private String placeId;

    @Indexed
    private FeedbackEventType eventType;

    @Indexed
    private Instant occurredAt;

    public FeedbackEvent(
        String id,
        String placeId,
        FeedbackEventType eventType,
        Instant occurredAt
    ) {
        this.id = id;
        this.placeId = placeId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    public String getId() {
        return id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public FeedbackEventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
