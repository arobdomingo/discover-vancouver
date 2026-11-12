package com.discovervancouver.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.discovervancouver.model.FeedbackEvent;
import com.discovervancouver.model.FeedbackEventType;

public interface FeedbackEventRepository
    extends MongoRepository<FeedbackEvent, String> {

    long countByEventType(FeedbackEventType eventType);
}
