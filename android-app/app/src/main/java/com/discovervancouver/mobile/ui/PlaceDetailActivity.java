package com.discovervancouver.mobile.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.data.PreferencesStore;
import com.discovervancouver.mobile.model.Recommendation;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.stream.Collectors;

public final class PlaceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECOMMENDATION =
        "com.discovervancouver.mobile.RECOMMENDATION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        Recommendation recommendation = getRecommendation();
        if (recommendation == null) {
            finish();
            return;
        }

        MaterialButton backButton = findViewById(R.id.backButton);
        MaterialButton mapsButton = findViewById(R.id.mapsButton);
        MaterialButton favouriteButton = findViewById(R.id.favouriteButton);
        TextView name = findViewById(R.id.detailName);
        TextView meta = findViewById(R.id.detailMeta);
        TextView description = findViewById(R.id.detailDescription);
        TextView address = findViewById(R.id.detailAddress);
        TextView reasons = findViewById(R.id.detailReasons);
        TextView tags = findViewById(R.id.detailTags);

        backButton.setOnClickListener(view -> finish());
        mapsButton.setOnClickListener(view -> openDirections(recommendation));
        PreferencesStore preferencesStore = new PreferencesStore(this);
        updateFavouriteButton(
            favouriteButton,
            preferencesStore.isFavourite(recommendation.getPlaceId())
        );
        favouriteButton.setOnClickListener(view -> {
            boolean favourite = preferencesStore.toggleFavourite(
                recommendation.getPlaceId()
            );
            updateFavouriteButton(favouriteButton, favourite);
            Toast.makeText(
                this,
                favourite
                    ? R.string.favourite_saved
                    : R.string.favourite_removed,
                Toast.LENGTH_SHORT
            ).show();
        });
        name.setText(recommendation.getName());
        meta.setText(String.format(
            Locale.CANADA,
            "%s • %d min walk • %s",
            recommendation.getCategory(),
            recommendation.getWalkingMinutes(),
            recommendation.isOpenNow() ? "Open now" : "Closed now"
        ));
        description.setText(recommendation.getDescription());
        address.setText(recommendation.getAddress());
        reasons.setText(recommendation.getReasons().stream()
            .map(reason -> "• " + reason)
            .collect(Collectors.joining("\n")));
        tags.setText(recommendation.getTags().stream()
            .map(tag -> "#" + tag)
            .collect(Collectors.joining("  ")));
    }

    private void openDirections(Recommendation recommendation) {
        Uri directions = Uri.parse(
            "https://www.google.com/maps/dir/?api=1"
        ).buildUpon()
            .appendQueryParameter(
                "destination",
                recommendation.getLatitude()
                    + ","
                    + recommendation.getLongitude()
            )
            .appendQueryParameter("travelmode", "walking")
            .build();
        Intent intent = new Intent(Intent.ACTION_VIEW, directions);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                this,
                R.string.maps_unavailable,
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void updateFavouriteButton(
        MaterialButton button,
        boolean favourite
    ) {
        button.setText(
            favourite ? R.string.remove_favourite : R.string.save_favourite
        );
    }

    @SuppressWarnings("deprecation")
    private Recommendation getRecommendation() {
        return (Recommendation) getIntent().getSerializableExtra(
            EXTRA_RECOMMENDATION
        );
    }
}
