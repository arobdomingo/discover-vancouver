package com.discovervancouver.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discovervancouver.mobile.BuildConfig;
import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.model.Recommendation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import static org.maplibre.android.style.layers.PropertyFactory.circleColor;
import static org.maplibre.android.style.layers.PropertyFactory.circleRadius;
import static org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor;
import static org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth;
import static org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap;
import static org.maplibre.android.style.layers.PropertyFactory.textColor;
import static org.maplibre.android.style.layers.PropertyFactory.textField;
import static org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement;
import static org.maplibre.android.style.layers.PropertyFactory.textSize;
import static org.maplibre.android.style.expressions.Expression.get;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecommendationMapActivity extends AppCompatActivity {

    private static final String EXTRA_RECOMMENDATIONS =
        "com.discovervancouver.mobile.RECOMMENDATION_MAP_RESULTS";
    private static final String RESULTS_SOURCE = "recommendation-results";
    private static final String SELECTED_SOURCE = "selected-recommendation";
    private static final String SELECTED_LAYER = "selected-marker-halo";
    private static final String RESULTS_LAYER = "recommendation-markers";
    private static final String RANK_LAYER = "recommendation-ranks";
    private static final String PLACE_ID_PROPERTY = "placeId";
    private static final String COLOR_PROPERTY = "markerColor";
    private static final String RANK_PROPERTY = "rank";
    private static final double SINGLE_RESULT_ZOOM = 15.0;
    private static final int CAMERA_PADDING_PIXELS = 140;

    private final Map<String, Integer> recommendationRanks = new HashMap<>();
    private MapView mapView;
    private MaterialCardView hintCard;
    private MaterialCardView previewCard;
    private TextView previewName;
    private TextView previewMeta;
    private TextView previewReason;
    private MaterialButton previewDetailsButton;
    private GeoJsonSource selectedSource;
    private Recommendation selectedRecommendation;

    public static Intent createIntent(
        Context context,
        List<Recommendation> recommendations
    ) {
        Intent intent = new Intent(context, RecommendationMapActivity.class);
        intent.putExtra(
            EXTRA_RECOMMENDATIONS,
            new ArrayList<>(recommendations)
        );
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_recommendation_map);

        MaterialToolbar toolbar = findViewById(R.id.mapToolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        bindPreviewViews();

        List<Recommendation> recommendations =
            RecommendationMapModel.mappable(readRecommendations());
        if (recommendations.isEmpty()) {
            Toast.makeText(this, R.string.map_no_results, Toast.LENGTH_LONG)
                .show();
            finish();
            return;
        }

        mapView = findViewById(R.id.recommendationMap);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> map.setStyle(
            BuildConfig.MAP_STYLE_URL,
            style -> showRecommendations(map, style, recommendations)
        ));
    }

    private void bindPreviewViews() {
        hintCard = findViewById(R.id.mapHintCard);
        previewCard = findViewById(R.id.mapPreviewCard);
        previewName = findViewById(R.id.mapPreviewName);
        previewMeta = findViewById(R.id.mapPreviewMeta);
        previewReason = findViewById(R.id.mapPreviewReason);
        previewDetailsButton = findViewById(R.id.mapPreviewDetailsButton);
        previewDetailsButton.setOnClickListener(view -> {
            if (selectedRecommendation != null) {
                openDetails(selectedRecommendation);
            }
        });
    }

    private void showRecommendations(
        MapLibreMap map,
        Style style,
        List<Recommendation> recommendations
    ) {
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        List<Feature> features = new ArrayList<>();
        for (int index = 0; index < recommendations.size(); index++) {
            Recommendation recommendation = recommendations.get(index);
            LatLng position = new LatLng(
                recommendation.getLatitude(),
                recommendation.getLongitude()
            );
            bounds.include(position);
            Feature feature = Feature.fromGeometry(Point.fromLngLat(
                recommendation.getLongitude(),
                recommendation.getLatitude()
            ));
            feature.addStringProperty(
                PLACE_ID_PROPERTY,
                recommendation.getPlaceId()
            );
            feature.addStringProperty(
                COLOR_PROPERTY,
                RecommendationMapModel.markerColor(
                    recommendation.getCategory()
                )
            );
            feature.addStringProperty(RANK_PROPERTY, Integer.toString(index + 1));
            recommendationRanks.put(recommendation.getPlaceId(), index + 1);
            features.add(feature);
        }

        style.addSource(new GeoJsonSource(
            RESULTS_SOURCE,
            FeatureCollection.fromFeatures(features)
        ));
        selectedSource = new GeoJsonSource(
            SELECTED_SOURCE,
            FeatureCollection.fromFeatures(new Feature[0])
        );
        style.addSource(selectedSource);
        style.addLayer(new CircleLayer(SELECTED_LAYER, SELECTED_SOURCE)
            .withProperties(
                circleColor("#5DE2CE"),
                circleRadius(17.0f),
                circleStrokeColor(Color.WHITE),
                circleStrokeWidth(3.0f)
            ));
        style.addLayer(new CircleLayer(RESULTS_LAYER, RESULTS_SOURCE)
            .withProperties(
                circleColor(get(COLOR_PROPERTY)),
                circleRadius(11.0f),
                circleStrokeColor(Color.WHITE),
                circleStrokeWidth(2.5f)
            ));
        style.addLayer(new SymbolLayer(RANK_LAYER, RESULTS_SOURCE)
            .withProperties(
                textField(get(RANK_PROPERTY)),
                textColor(Color.WHITE),
                textSize(12.0f),
                textAllowOverlap(true),
                textIgnorePlacement(true)
            ));

        map.addOnMapClickListener(point -> {
            List<Feature> selectedFeatures = map.queryRenderedFeatures(
                map.getProjection().toScreenLocation(point),
                RESULTS_LAYER
            );
            if (selectedFeatures.isEmpty()) {
                clearSelection();
                return false;
            }
            String placeId = selectedFeatures.get(0).getStringProperty(
                PLACE_ID_PROPERTY
            );
            Recommendation selected = findRecommendation(
                recommendations,
                placeId
            );
            if (selected == null) {
                return false;
            }
            selectRecommendation(selected, selectedFeatures.get(0));
            return true;
        });

        mapView.post(() -> moveCamera(map, recommendations, bounds));
    }

    void selectRecommendation(
        Recommendation recommendation,
        Feature selectedFeature
    ) {
        selectedRecommendation = recommendation;
        if (selectedSource != null) {
            selectedSource.setGeoJson(FeatureCollection.fromFeature(
                selectedFeature
            ));
        }
        previewName.setText(recommendation.getName());
        previewMeta.setText(previewMeta(recommendation));
        previewReason.setText(recommendation.getReasons().isEmpty()
            ? recommendation.getDescription()
            : recommendation.getReasons().get(0));
        hintCard.setVisibility(View.GONE);
        previewCard.setVisibility(View.VISIBLE);
    }

    private void clearSelection() {
        selectedRecommendation = null;
        if (selectedSource != null) {
            selectedSource.setGeoJson(FeatureCollection.fromFeatures(
                new Feature[0]
            ));
        }
        previewCard.setVisibility(View.GONE);
        hintCard.setVisibility(View.VISIBLE);
    }

    private String previewMeta(Recommendation recommendation) {
        String hours = recommendation.isHoursKnown()
            ? getString(recommendation.isOpenNow()
                ? R.string.hours_open_now
                : R.string.hours_closed_now)
            : getString(R.string.hours_unavailable);
        return getString(
            R.string.map_preview_meta,
            recommendationRank(recommendation),
            recommendation.getCategory(),
            recommendation.getWalkingMinutes(),
            hours
        );
    }

    private int recommendationRank(Recommendation recommendation) {
        return recommendationRanks.getOrDefault(
            recommendation.getPlaceId(),
            1
        );
    }

    private void openDetails(Recommendation recommendation) {
        Intent detailIntent = new Intent(this, PlaceDetailActivity.class);
        detailIntent.putExtra(
            PlaceDetailActivity.EXTRA_RECOMMENDATION,
            recommendation
        );
        startActivity(detailIntent);
    }

    private Recommendation findRecommendation(
        List<Recommendation> recommendations,
        String placeId
    ) {
        for (Recommendation recommendation : recommendations) {
            if (recommendation.getPlaceId().equals(placeId)) {
                return recommendation;
            }
        }
        return null;
    }

    private void moveCamera(
        MapLibreMap map,
        List<Recommendation> recommendations,
        LatLngBounds.Builder bounds
    ) {
        if (recommendations.size() == 1) {
            Recommendation result = recommendations.get(0);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(result.getLatitude(), result.getLongitude()),
                SINGLE_RESULT_ZOOM
            ));
            return;
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
            bounds.build(),
            CAMERA_PADDING_PIXELS
        ));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private List<Recommendation> readRecommendations() {
        Serializable value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = getIntent().getSerializableExtra(
                EXTRA_RECOMMENDATIONS,
                ArrayList.class
            );
        } else {
            value = getIntent().getSerializableExtra(EXTRA_RECOMMENDATIONS);
        }
        if (!(value instanceof ArrayList<?> values)) {
            return List.of();
        }

        List<Recommendation> recommendations = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Recommendation recommendation) {
                recommendations.add(recommendation);
            }
        }
        return recommendations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
}
