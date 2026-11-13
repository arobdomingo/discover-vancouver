package com.discovervancouver.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.place-sources")
public class PlaceSourceProperties {

    private int batchSize = 500;
    private String reportFile = "output/place-data-quality-report.json";
    private final Foursquare foursquare = new Foursquare();
    private final Vancouver vancouver = new Vancouver();

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public String getReportFile() { return reportFile; }
    public void setReportFile(String reportFile) { this.reportFile = reportFile; }
    public Foursquare getFoursquare() { return foursquare; }
    public Vancouver getVancouver() { return vancouver; }

    public static class Foursquare {
        private boolean enabled;
        private String file = "";
        private double minLatitude = 49.0;
        private double maxLatitude = 49.5;
        private double minLongitude = -123.5;
        private double maxLongitude = -122.5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public double getMinLatitude() { return minLatitude; }
        public void setMinLatitude(double value) { minLatitude = value; }
        public double getMaxLatitude() { return maxLatitude; }
        public void setMaxLatitude(double value) { maxLatitude = value; }
        public double getMinLongitude() { return minLongitude; }
        public void setMinLongitude(double value) { minLongitude = value; }
        public double getMaxLongitude() { return maxLongitude; }
        public void setMaxLongitude(double value) { maxLongitude = value; }
    }

    public static class Vancouver {
        private boolean enabled;
        private String baseUrl = "https://opendata.vancouver.ca";
        private int pageSize = 100;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(20);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration value) { connectTimeout = value; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration value) { readTimeout = value; }
    }
}
