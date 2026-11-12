package com.discovervancouver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public String health(){
        return "Discover Vancouver API is running";
    }
    
}
