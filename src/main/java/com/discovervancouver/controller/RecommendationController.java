package com.discovervancouver.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.discovervancouver.dto.RecommendationResponse;
import com.discovervancouver.service.RecommendationService;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService){
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationResponse> getRecommendations(
        @RequestParam
        @DecimalMin(value = "-90.0", message = "lat must be at least -90")
        @DecimalMax(value = "90.0", message = "lat must be at most 90")
        double lat,

        @RequestParam
        @DecimalMin(value = "-180.0", message = "lng must be at least -180")
        @DecimalMax(value = "180.0", message = "lng must be at most 180")
        double lng,

        @RequestParam(required = false)
        @Size(max = 50, message = "category must contain at most 50 characters")
        String category,

        @RequestParam(required = false)
        Boolean indoorPreferred,

        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "maxWalkingMinutes must be at least 1")
        @Max(value = 120, message = "maxWalkingMinutes must be at most 120")
        //consider the effects of this choice (max walking is less than 2 hrs)
        int maxWalkingMinutes,

        @RequestParam(required = false)
        @Pattern(
            regexp = "(?i)free|low|medium|high",
            message = "budgetPreference must be free, low, medium or high")
        String budgetPreference,

        @RequestParam(required = false)
        @Size(max = 50, message = "atmosphere must contain at most 50 characters")
        String atmosphere,

        @RequestParam(defaultValue = "false")
        boolean openOnly,

        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "limit must be at least 1")
        @Max(value = 50, message = "limit must be at most 50")
        int limit
    ){
        return recommendationService.getRecommendations(lat, lng, category, indoorPreferred, maxWalkingMinutes, budgetPreference, atmosphere, openOnly, limit);
    }
    
}
