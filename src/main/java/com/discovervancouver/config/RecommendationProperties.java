package com.discovervancouver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.recommendation")
public class RecommendationProperties {

    private int defaultLimit = 10;
    private int maximumLimit = 50;
    private final Weights weights = new Weights();

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaximumLimit() {
        return maximumLimit;
    }

    public void setMaximumLimit(int maximumLimit) {
        this.maximumLimit = maximumLimit;
    }

    public Weights getWeights() {
        return weights;
    }

    public static class Weights {
        private int category = 25;
        private int indoor = 15;
        private int openNow = 20;
        private int budget = 10;
        private int atmosphere = 12;
        private int rainyIndoor = 10;
        private int rainyDayTag = 10;
        private int timeOfDay = 8;
        private int distanceMaximum = 20;
        private int dataQuality = 8;
        private int completeness = 6;

        public int getCategory() { return category; }
        public void setCategory(int category) { this.category = category; }
        public int getIndoor() { return indoor; }
        public void setIndoor(int indoor) { this.indoor = indoor; }
        public int getOpenNow() { return openNow; }
        public void setOpenNow(int openNow) { this.openNow = openNow; }
        public int getBudget() { return budget; }
        public void setBudget(int budget) { this.budget = budget; }
        public int getAtmosphere() { return atmosphere; }
        public void setAtmosphere(int atmosphere) { this.atmosphere = atmosphere; }
        public int getRainyIndoor() { return rainyIndoor; }
        public void setRainyIndoor(int rainyIndoor) { this.rainyIndoor = rainyIndoor; }
        public int getRainyDayTag() { return rainyDayTag; }
        public void setRainyDayTag(int rainyDayTag) { this.rainyDayTag = rainyDayTag; }
        public int getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(int timeOfDay) { this.timeOfDay = timeOfDay; }
        public int getDistanceMaximum() { return distanceMaximum; }
        public void setDistanceMaximum(int distanceMaximum) { this.distanceMaximum = distanceMaximum; }
        public int getDataQuality() { return dataQuality; }
        public void setDataQuality(int dataQuality) { this.dataQuality = dataQuality; }
        public int getCompleteness() { return completeness; }
        public void setCompleteness(int completeness) { this.completeness = completeness; }
    }
}
