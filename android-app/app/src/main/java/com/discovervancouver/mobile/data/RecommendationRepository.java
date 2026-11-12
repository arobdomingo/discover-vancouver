package com.discovervancouver.mobile.data;

import com.discovervancouver.mobile.api.RecommendationApiClient;
import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public final class RecommendationRepository {

    public interface Callback {
        void onSuccess(List<Recommendation> recommendations);
        void onError(String message);
    }

    private final RecommendationApiClient apiClient;

    public RecommendationRepository(String baseUrl) {
        apiClient = new RecommendationApiClient(baseUrl);
    }

    public void getRecommendations(
        RecommendationQuery query,
        Callback callback
    ) {
        apiClient.getRecommendations(
            query,
            new RecommendationApiClient.Callback() {
                @Override
                public void onSuccess(
                    List<Recommendation> recommendations
                ) {
                    callback.onSuccess(recommendations);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    public void close() {
        apiClient.shutdown();
    }
}
