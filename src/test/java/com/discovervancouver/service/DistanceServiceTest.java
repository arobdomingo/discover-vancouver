package com.discovervancouver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DistanceServiceTest {
    
    private final DistanceService distanceService = new DistanceService();

    @Test
    void distanceBetweenSamePointIsZero(){
        int distance = distanceService.distanceMeters(49.2827, -123.1207, 49.2827, -123.1207);
        assertEquals(0, distance);
    }

    @Test
    void walkingMinutesIsAtLeastOne(){
        int walkingMinutes = distanceService.walkingMinutes(0);
        assertEquals(1, walkingMinutes);
    }

    @Test
    void maximumWalkingDistanceUsesSameWalkingSpeed() {
        assertEquals(
            1_600,
            distanceService.maximumWalkingDistanceMeters(20)
        );
    }

    @Test
    void distanceBetweenDowntownPointsIsReasonable(){
        int distance = distanceService.distanceMeters(49.2827, -123.1207, 49.2810, -123.1170);
        assertTrue(distance > 200);
        assertTrue(distance < 500);
    }

}
