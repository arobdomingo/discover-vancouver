package com.discovervancouver.mobile.api;

import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public interface RecommendationDataSource {

    interface Callback {
        void onSuccess(List<Recommendation> recommendations);
        void onError(String message);
    }

    void getRecommendations(RecommendationQuery query, Callback callback);

    void close();
}
