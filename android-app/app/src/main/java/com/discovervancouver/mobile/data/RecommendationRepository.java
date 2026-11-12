package com.discovervancouver.mobile.data;

import com.discovervancouver.mobile.api.RecommendationApiClient;
import com.discovervancouver.mobile.api.RecommendationDataSource;
import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public final class RecommendationRepository {

    public interface Callback {
        void onSuccess(List<Recommendation> recommendations);
        void onError(String message);
    }

    private final RecommendationDataSource dataSource;

    public RecommendationRepository(String baseUrl) {
        this(new RecommendationApiClient(baseUrl));
    }

    public RecommendationRepository(RecommendationDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void getRecommendations(
        RecommendationQuery query,
        Callback callback
    ) {
        dataSource.getRecommendations(
            query,
            new RecommendationDataSource.Callback() {
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
        dataSource.close();
    }
}
