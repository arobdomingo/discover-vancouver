package com.discovervancouver.mobile.api;

import android.os.Handler;
import android.os.Looper;

import com.discovervancouver.mobile.model.Recommendation;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RecommendationApiClient {

    public interface Callback {
        void onSuccess(List<Recommendation> recommendations);
        void onError(String message);
    }

    private final String baseUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RecommendationApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public void getRecommendations(
        RecommendationQuery query,
        Callback callback
    ) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(
                    baseUrl
                        + "api/v1/recommendations?"
                        + query.toQueryString()
                );
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(8_000);
                connection.setRequestProperty("Accept", "application/json");

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
                String body = readBody(stream);

                if (status < 200 || status >= 300) {
                    throw new IOException(
                        "The server returned HTTP " + status + ": " + body
                    );
                }

                List<Recommendation> recommendations = parse(body);
                mainHandler.post(() -> callback.onSuccess(recommendations));
            } catch (Exception exception) {
                String message = exception.getMessage() == null
                    ? "Could not reach the Discover Vancouver server."
                    : exception.getMessage();
                mainHandler.post(() -> callback.onError(message));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private List<Recommendation> parse(String body) throws Exception {
        JSONArray array = new JSONArray(body);
        List<Recommendation> recommendations = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            recommendations.add(
                Recommendation.fromJson(array.getJSONObject(index))
            );
        }
        return recommendations;
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
}
