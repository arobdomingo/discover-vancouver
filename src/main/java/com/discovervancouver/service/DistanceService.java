package com.discovervancouver.service;

import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private static final double Earth_RADIUS_METERS = 6_371_000;
    private static final int AVERAGE_WALKING_METERS_PER_MINUTE = 80;

    public int distanceMeters(double fromLat, double fromLng, double toLat, double toLng){
        double fromLatRadians = Math.toRadians(fromLat);
        double toLatRadians = Math.toRadians(toLat);

        double latDelta = Math.toRadians(toLat - fromLat);
        double lngDelta = Math.toRadians(toLng - fromLng);


        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(fromLatRadians) * Math.cos(toLatRadians)
                * Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); 

        return (int)Math.round(Earth_RADIUS_METERS * c);
    }

    public int walkingMinutes(int distanceMeters) {
        return Math.max(
            1,
            (int) Math.ceil(
                (double) distanceMeters
                    / AVERAGE_WALKING_METERS_PER_MINUTE
            )
        );
    }

    public int maximumWalkingDistanceMeters(int walkingMinutes) {
        return walkingMinutes * AVERAGE_WALKING_METERS_PER_MINUTE;
    }
    
}
