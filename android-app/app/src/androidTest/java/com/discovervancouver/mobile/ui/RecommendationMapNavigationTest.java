package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.model.Recommendation;

import org.maplibre.android.maps.MapView;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.Point;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class RecommendationMapNavigationTest {

    @Test
    public void mapScreenLoadsStreetBasemapWithRecommendationResults()
        throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = RecommendationMapActivity.createIntent(
            context,
            List.of(recommendation())
        );

        CountDownLatch styleLoaded = new CountDownLatch(1);
        AtomicBoolean hasBasemapLayers = new AtomicBoolean(false);

        try (ActivityScenario<RecommendationMapActivity> scenario =
                 ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                MapView mapView = activity.findViewById(
                    R.id.recommendationMap
                );
                assertNotNull(mapView);
                mapView.getMapAsync(map -> map.getStyle(style -> {
                    hasBasemapLayers.set(style.getLayers().size() > 20);
                    styleLoaded.countDown();
                }));
            });
            assertTrue(styleLoaded.await(30, TimeUnit.SECONDS));
            assertTrue(hasBasemapLayers.get());
            scenario.onActivity(activity -> {
                Recommendation result = recommendation();
                activity.selectRecommendation(
                    result,
                    Feature.fromGeometry(Point.fromLngLat(
                        result.getLongitude(),
                        result.getLatitude()
                    ))
                );
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById(R.id.mapPreviewCard)
                        .getVisibility()
                );
                TextView name = activity.findViewById(R.id.mapPreviewName);
                assertEquals(result.getName(), name.getText().toString());
            });
        }
    }

    private Recommendation recommendation() {
        return new Recommendation(
            "map-place-1",
            "Vancouver Public Library",
            "study",
            "Central library",
            "350 West Georgia Street",
            0,
            true,
            true,
            List.of("library", "quiet"),
            90,
            500,
            7,
            49.2797,
            -123.1156,
            false,
            List.of("Matches your study preference")
        );
    }
}
