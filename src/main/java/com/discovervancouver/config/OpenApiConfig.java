package com.discovervancouver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI discoverVancouverOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Discover Vancouver API")
            .version("v1")
            .description(
                "Location-aware place recommendations for Vancouver"
            ));
    }
}
