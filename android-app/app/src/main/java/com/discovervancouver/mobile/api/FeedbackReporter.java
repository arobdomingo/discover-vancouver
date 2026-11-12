package com.discovervancouver.mobile.api;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FeedbackReporter {

    public enum EventType {
        RECOMMENDATION_VIEWED,
        FAVORITED,
        UNFAVORITED,
        DIRECTIONS_OPENED
    }

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor();

    private FeedbackReporter() {
    }

    public static void report(
        String baseUrl,
        String placeId,
        EventType eventType
    ) {
        EXECUTOR.execute(() -> send(baseUrl, placeId, eventType));
    }

    private static void send(
        String baseUrl,
        String placeId,
        EventType eventType
    ) {
        HttpURLConnection connection = null;
        try {
            String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl
                : baseUrl + "/";
            connection = (HttpURLConnection) new URL(
                normalizedBase + "api/feedback"
            ).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3_000);
            connection.setReadTimeout(3_000);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            );

            byte[] body = new JSONObject()
                .put("placeId", placeId)
                .put("eventType", eventType.name())
                .toString()
                .getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            connection.getResponseCode();
        } catch (Exception ignored) {
            // Analytics must never interrupt the discovery experience.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
