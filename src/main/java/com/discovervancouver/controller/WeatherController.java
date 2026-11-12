package com.discovervancouver.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.discovervancouver.dto.WeatherResponse;
import com.discovervancouver.service.WeatherService;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {
    
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService){
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public WeatherResponse getCurrentWeather(
        @RequestParam
        @DecimalMin(value = "-90.0", message = "lat must be at least -90")
        @DecimalMax(value = "90.0", message = "lat must be at most 90")
        double lat,

        @RequestParam
        @DecimalMin(value = "-180.0", message = "lng must be at least -180")
        @DecimalMax(value = "180.0", message = "lng must be at most 180")
        double lng
    ) {
        return weatherService.getCurrentWeatherResponse(lat, lng);
    }

}
