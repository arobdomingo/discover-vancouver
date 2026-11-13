package com.discovervancouver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public final class PlaceIngestionApplication {
    private PlaceIngestionApplication() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(
            DiscoverVancouverApplication.class
        );
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("ingestion");
        try (ConfigurableApplicationContext ignored = application.run(args)) {
            // ApplicationRunner beans finish the one-shot import before run returns.
        }
    }
}
