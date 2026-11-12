package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.discovervancouver.mobile.TestRecommendations;
import com.discovervancouver.mobile.api.RecommendationDataSource;
import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.data.RecommendationRepository;
import com.discovervancouver.mobile.model.Recommendation;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class MainViewModelTest {

    @Rule
    public final InstantTaskExecutorRule executorRule =
        new InstantTaskExecutorRule();

    @Test
    public void publishesLoadingThenSuccess() {
        MainViewModel viewModel = viewModel(
            List.of(TestRecommendations.sample()),
            null
        );

        viewModel.loadRecommendations(query());

        DiscoveryUiState state = viewModel.getState().getValue();
        assertEquals(DiscoveryUiState.Status.SUCCESS, state.status());
        assertEquals("place-1", state.recommendations().get(0).getPlaceId());
    }

    @Test
    public void publishesOfflineError() {
        MainViewModel viewModel = viewModel(List.of(), "Network unavailable");

        viewModel.loadRecommendations(query());

        DiscoveryUiState state = viewModel.getState().getValue();
        assertEquals(DiscoveryUiState.Status.ERROR, state.status());
        assertEquals("Network unavailable", state.errorMessage());
    }

    private MainViewModel viewModel(
        List<Recommendation> results,
        String error
    ) {
        RecommendationDataSource dataSource = new RecommendationDataSource() {
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
        };
        return new MainViewModel(new RecommendationRepository(dataSource));
    }

    private RecommendationQuery query() {
        return new RecommendationQuery(
            49.2827, -123.1207, null, null, 20,
            null, null, false, 10
        );
    }
}
