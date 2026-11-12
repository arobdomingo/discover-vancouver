package com.discovervancouver.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.discovervancouver.mobile.TestRecommendations;
import com.discovervancouver.mobile.api.RecommendationDataSource;
import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.model.Recommendation;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecommendationRepositoryTest {

    @Test
    public void forwardsSuccessfulResultsFromApi() {
        RecommendationRepository repository = new RecommendationRepository(
            new FakeDataSource(List.of(TestRecommendations.sample()), null)
        );
        AtomicReference<List<Recommendation>> received =
            new AtomicReference<>();

        repository.getRecommendations(query(), new RecommendationRepository.Callback() {
            @Override
            public void onSuccess(List<Recommendation> recommendations) {
                received.set(recommendations);
            }

            @Override
            public void onError(String message) { }
        });

        assertEquals("place-1", received.get().get(0).getPlaceId());
    }

    @Test
    public void forwardsOfflineErrorsFromApi() {
        RecommendationRepository repository = new RecommendationRepository(
            new FakeDataSource(List.of(), "Network unavailable")
        );
        AtomicReference<String> error = new AtomicReference<>();

        repository.getRecommendations(query(), new RecommendationRepository.Callback() {
            @Override
            public void onSuccess(List<Recommendation> recommendations) { }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertTrue(error.get().contains("Network unavailable"));
    }

    private RecommendationQuery query() {
        return new RecommendationQuery(
            49.2827, -123.1207, "cafe", null, 20,
            null, null, false, 10
        );
    }

    private record FakeDataSource(
        List<Recommendation> results,
        String error
    ) implements RecommendationDataSource {
        @Override
        public void getRecommendations(
            RecommendationQuery query,
            Callback callback
        ) {
            if (error == null) {
                callback.onSuccess(results);
            } else {
                callback.onError(error);
            }
        }

        @Override
        public void close() { }
    }
}
