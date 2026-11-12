package com.discovervancouver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.discovervancouver.dto.WeatherResponse;
import com.discovervancouver.exception.WeatherUnavailableException;
import com.sun.net.httpserver.HttpServer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class WeatherServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsSuccessfulProviderResponse() throws Exception {
        startServer(200, """
            {"current":{
              "temperature_2m":8.5,
              "precipitation":1.2,
              "rain":1.0,
              "weather_code":61
            }}
            """);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WeatherService service = service(registry);

        WeatherResponse weather = service.getCurrentWeatherResponse(
            49.2827,
            -123.1207
        );

        assertThat(weather.temperature()).isEqualTo(8.5);
        assertThat(weather.rainy()).isTrue();
        assertThat(registry.counter("discover.weather.provider.failures").count())
            .isZero();
    }

    @Test
    void recordsFailureForMalformedProviderResponse() throws Exception {
        startServer(200, "{\"current\":{\"temperature_2m\":\"warm\"}}");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WeatherService service = service(registry);

        assertThatThrownBy(() -> service.getCurrentWeatherResponse(
            49.2827,
            -123.1207
        )).isInstanceOf(WeatherUnavailableException.class);
        assertThat(registry.counter("discover.weather.provider.failures").count())
            .isEqualTo(1.0);
    }

    @Test
    void recordsFailureForProviderServerError() throws Exception {
        startServer(503, "temporarily unavailable");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThatThrownBy(() -> service(registry).getCurrentWeatherResponse(
            49.2827,
            -123.1207
        )).isInstanceOf(WeatherUnavailableException.class);
        assertThat(registry.counter("discover.weather.provider.failures").count())
            .isEqualTo(1.0);
    }

    private WeatherService service(SimpleMeterRegistry registry) {
        return new WeatherService(
            "http://127.0.0.1:" + server.getAddress().getPort(),
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            registry
        );
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/forecast", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json"
            );
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }
}
