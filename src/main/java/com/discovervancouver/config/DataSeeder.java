package com.discovervancouver.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

import com.discovervancouver.model.Place;
import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.repository.PlaceRepository;

@Configuration
@Profile("dev")
@ConditionalOnProperty(
    prefix = "app.seed-data",
    name = "enabled",
    havingValue = "true"
)
public class DataSeeder {
    
    @Bean
    @Order(0)
    CommandLineRunner seedPlaces(PlaceRepository placeRepository){
        return args -> {
            List<Place> seedPlaces = List.of(
                new Place(
                        "place1",
                        "Breka Bakery & Cafe",
                        "cafe",
                        "Casual cafe with pastries and study-friendly seating.",
                        "855 Davie St, Vancouver, BC",
                        1,
                        true,
                        allDayEveryDay(),
                        49.2810,
                        -123.1170,
                        List.of("cafe", "study", "casual", "late-night", "wifi")
                ),
                new Place(
                        "place2",
                        "Vancouver Public Library Central",
                        "study",
                        "Iconic central library with quiet indoor work areas.",
                        "350 W Georgia St, Vancouver, BC",
                        0,
                        true,
                        libraryHours(),
                        49.2797,
                        -123.1156,
                        List.of("study", "quiet", "free", "rainy-day", "indoor")
                ),
                new Place(
                        "place3",
                        "Nelson Park",
                        "park",
                        "Small downtown green space for a calm outdoor reset.",
                        "1030 Bute St, Vancouver, BC",
                        0,
                        false,
                        parkHours(),
                        49.2813,
                        -123.1308,
                        List.of("park", "quiet", "walk", "free", "outdoor")
                )
            );

            if(placeRepository.count() == 0) {
                placeRepository.saveAll(seedPlaces);
                return;
            }

            placeRepository.findAll().stream()
                .filter(place -> place.getLocation() == null)
                .forEach(place -> {
                    place.synchronizeLocation();
                    placeRepository.save(place);
                });

            seedPlaces.forEach(seedPlace -> placeRepository
                .findById(seedPlace.getId())
                .filter(existingPlace ->
                    existingPlace.getOpeningHours() == null
                )
                .ifPresent(existingPlace -> {
                    existingPlace.setOpeningHours(
                        seedPlace.getOpeningHours()
                    );
                    placeRepository.save(existingPlace);
                }));

        };
    }

    private static Map<DayOfWeek, List<OpeningPeriod>> allDayEveryDay() {
        List<OpeningPeriod> allDay = List.of(
            new OpeningPeriod(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)
        );

        return Map.of(
            DayOfWeek.MONDAY, allDay,
            DayOfWeek.TUESDAY, allDay,
            DayOfWeek.WEDNESDAY, allDay,
            DayOfWeek.THURSDAY, allDay,
            DayOfWeek.FRIDAY, allDay,
            DayOfWeek.SATURDAY, allDay,
            DayOfWeek.SUNDAY, allDay
        );
    }

    private static Map<DayOfWeek, List<OpeningPeriod>> libraryHours() {
        List<OpeningPeriod> weekday = List.of(
            new OpeningPeriod(LocalTime.of(9, 30), LocalTime.of(20, 30))
        );
        List<OpeningPeriod> weekend = List.of(
            new OpeningPeriod(LocalTime.of(10, 0), LocalTime.of(18, 0))
        );

        return Map.of(
            DayOfWeek.MONDAY, weekday,
            DayOfWeek.TUESDAY, weekday,
            DayOfWeek.WEDNESDAY, weekday,
            DayOfWeek.THURSDAY, weekday,
            DayOfWeek.FRIDAY, weekday,
            DayOfWeek.SATURDAY, weekend,
            DayOfWeek.SUNDAY, weekend
        );
    }

    private static Map<DayOfWeek, List<OpeningPeriod>> parkHours() {
        List<OpeningPeriod> daily = List.of(
            new OpeningPeriod(LocalTime.of(6, 0), LocalTime.of(22, 0))
        );

        return Map.of(
            DayOfWeek.MONDAY, daily,
            DayOfWeek.TUESDAY, daily,
            DayOfWeek.WEDNESDAY, daily,
            DayOfWeek.THURSDAY, daily,
            DayOfWeek.FRIDAY, daily,
            DayOfWeek.SATURDAY, daily,
            DayOfWeek.SUNDAY, daily
        );
    }

}
