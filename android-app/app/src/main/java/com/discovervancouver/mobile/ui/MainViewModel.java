package com.discovervancouver.mobile.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.data.RecommendationRepository;
import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public final class MainViewModel extends ViewModel {

    private static final double DEFAULT_LATITUDE = 49.2827;
    private static final double DEFAULT_LONGITUDE = -123.1207;

    private final RecommendationRepository repository;
    private final MutableLiveData<DiscoveryUiState> state =
        new MutableLiveData<>(DiscoveryUiState.idle());

    private double latitude = DEFAULT_LATITUDE;
    private double longitude = DEFAULT_LONGITUDE;
    private boolean usingDeviceLocation;

    public MainViewModel(RecommendationRepository repository) {
        this.repository = repository;
    }

    public LiveData<DiscoveryUiState> getState() {
        return state;
    }

    public void loadRecommendations(RecommendationQuery query) {
        state.setValue(DiscoveryUiState.loading());
        repository.getRecommendations(
            query,
            new RecommendationRepository.Callback() {
                @Override
                public void onSuccess(
                    List<Recommendation> recommendations
                ) {
                    state.setValue(
                        DiscoveryUiState.success(recommendations)
                    );
                }

                @Override
                public void onError(String message) {
                    state.setValue(DiscoveryUiState.error(message));
                }
            }
        );
    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        usingDeviceLocation = true;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isUsingDeviceLocation() {
        return usingDeviceLocation;
    }

    @Override
    protected void onCleared() {
        repository.close();
    }

    public static final class Factory implements ViewModelProvider.Factory {

        private final String baseUrl;

        public Factory(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(MainViewModel.class)) {
                throw new IllegalArgumentException(
                    "Unknown ViewModel class: " + modelClass.getName()
                );
            }
            return (T) new MainViewModel(
                new RecommendationRepository(baseUrl)
            );
        }
    }
}
