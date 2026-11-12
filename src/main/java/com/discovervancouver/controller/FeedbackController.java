package com.discovervancouver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.discovervancouver.dto.CreateFeedbackRequest;
import com.discovervancouver.dto.FeedbackSummaryResponse;
import com.discovervancouver.service.FeedbackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<Void> record(
        @Valid @RequestBody CreateFeedbackRequest request
    ) {
        feedbackService.record(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/summary")
    public FeedbackSummaryResponse summary() {
        return feedbackService.summarize();
    }
}
