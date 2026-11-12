package com.discovervancouver.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.discovervancouver.dto.CreateFeedbackRequest;
import com.discovervancouver.dto.FeedbackSummaryResponse;
import com.discovervancouver.model.FeedbackEvent;
import com.discovervancouver.model.FeedbackEventType;
import com.discovervancouver.repository.FeedbackEventRepository;

@Service
public class FeedbackService {

    private final FeedbackEventRepository repository;
    private final Clock clock;

    public FeedbackService(
        FeedbackEventRepository repository,
        Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(CreateFeedbackRequest request) {
        repository.save(new FeedbackEvent(
            null,
            request.placeId().trim(),
            request.eventType(),
            Instant.now(clock)
        ));
    }

    public FeedbackSummaryResponse summarize() {
        Map<FeedbackEventType, Long> counts =
            new EnumMap<>(FeedbackEventType.class);
        for (FeedbackEventType eventType : FeedbackEventType.values()) {
            counts.put(
                eventType,
                repository.countByEventType(eventType)
            );
        }
        return new FeedbackSummaryResponse(
            repository.count(),
            Map.copyOf(counts)
        );
    }
}
