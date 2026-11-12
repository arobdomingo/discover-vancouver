package com.discovervancouver.mobile.ui;

import com.discovervancouver.mobile.model.Recommendation;

import java.util.List;

public record DiscoveryUiState(
    Status status,
    List<Recommendation> recommendations,
    String errorMessage
) {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public DiscoveryUiState {
        recommendations = List.copyOf(recommendations);
    }

    public static DiscoveryUiState idle() {
        return new DiscoveryUiState(Status.IDLE, List.of(), null);
    }

    public static DiscoveryUiState loading() {
        return new DiscoveryUiState(Status.LOADING, List.of(), null);
    }

    public static DiscoveryUiState success(
        List<Recommendation> recommendations
    ) {
        return new DiscoveryUiState(Status.SUCCESS, recommendations, null);
    }

    public static DiscoveryUiState error(String message) {
        return new DiscoveryUiState(Status.ERROR, List.of(), message);
    }
}
