package com.discovervancouver.mobile.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.discovervancouver.mobile.BuildConfig;
import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.api.RecommendationQuery;
import com.discovervancouver.mobile.data.DiscoveryPreferences;
import com.discovervancouver.mobile.data.PreferencesStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends AppCompatActivity {

    private static final long LOCATION_TIMEOUT_MILLIS = 10_000L;

    private MainViewModel viewModel;
    private PreferencesStore preferencesStore;
    private RecommendationAdapter adapter;
    private Spinner categorySpinner;
    private Spinner budgetSpinner;
    private TextInputEditText atmosphereInput;
    private MaterialSwitch indoorSwitch;
    private MaterialSwitch openOnlySwitch;
    private SeekBar walkingRangeSeekBar;
    private TextView walkingRangeLabel;
    private TextView locationStatus;
    private TextView resultsHeading;
    private TextView messageView;
    private ProgressBar progressBar;
    private MaterialButton discoverButton;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CancellationSignal locationCancellationSignal;
    private LocationListener locationListener;
    private Runnable locationTimeout;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    requestFreshLocation();
                } else {
                    locationStatus.setText(R.string.location_permission_denied);
                }
            }
        );

    private final ActivityResultLauncher<String>
        localNetworkPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    onDiscoverClicked();
                } else {
                    messageView.setText(
                        R.string.local_network_permission_denied
                    );
                    messageView.setVisibility(View.VISIBLE);
                }
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesStore = new PreferencesStore(this);
        viewModel = new ViewModelProvider(
            this,
            new MainViewModel.Factory(BuildConfig.API_BASE_URL)
        ).get(MainViewModel.class);

        bindViews();
        configureInputs();
        viewModel.getState().observe(this, this::renderState);

        if (viewModel.isUsingDeviceLocation()) {
            locationStatus.setText(R.string.location_ready);
        }
    }

    private void bindViews() {
        categorySpinner = findViewById(R.id.categorySpinner);
        budgetSpinner = findViewById(R.id.budgetSpinner);
        atmosphereInput = findViewById(R.id.atmosphereInput);
        indoorSwitch = findViewById(R.id.indoorSwitch);
        openOnlySwitch = findViewById(R.id.openOnlySwitch);
        walkingRangeSeekBar = findViewById(R.id.walkingRangeSeekBar);
        walkingRangeLabel = findViewById(R.id.walkingRangeLabel);
        locationStatus = findViewById(R.id.locationStatus);
        resultsHeading = findViewById(R.id.resultsHeading);
        messageView = findViewById(R.id.messageView);
        progressBar = findViewById(R.id.progressBar);
        discoverButton = findViewById(R.id.discoverButton);

        RecyclerView resultsList = findViewById(R.id.resultsList);
        adapter = new RecommendationAdapter(preferencesStore);
        resultsList.setLayoutManager(new LinearLayoutManager(this));
        resultsList.setAdapter(adapter);
    }

    private void configureInputs() {
        setSpinnerValues(
            categorySpinner,
            List.of(
                "Any category",
                "Cafe",
                "Food",
                "Park",
                "Study",
                "Shopping",
                "Nightlife"
            )
        );
        setSpinnerValues(
            budgetSpinner,
            List.of("Any budget", "Free", "Low", "Medium", "High")
        );

        applySavedPreferences(preferencesStore.loadDiscoveryPreferences());
        updateWalkingRangeLabel();
        walkingRangeSeekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar,
                    int progress,
                    boolean fromUser
                ) {
                    updateWalkingRangeLabel();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            }
        );

        findViewById(R.id.locationButton).setOnClickListener(
            view -> requestLocation()
        );
        discoverButton.setOnClickListener(view -> onDiscoverClicked());
    }

    private void setSpinnerValues(Spinner spinner, List<String> values) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            values
        );
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        spinner.setAdapter(spinnerAdapter);
    }

    private void applySavedPreferences(DiscoveryPreferences preferences) {
        categorySpinner.setSelection(preferences.categoryPosition());
        budgetSpinner.setSelection(preferences.budgetPosition());
        atmosphereInput.setText(preferences.atmosphere());
        indoorSwitch.setChecked(preferences.indoorPreferred());
        openOnlySwitch.setChecked(preferences.openOnly());
        walkingRangeSeekBar.setProgress(
            preferences.walkingRangeProgress()
        );
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            requestFreshLocation();
            return;
        }

        locationPermissionLauncher.launch(
            Manifest.permission.ACCESS_COARSE_LOCATION
        );
    }

    @SuppressWarnings("deprecation")
    private void requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        cancelLocationRequest();
        LocationManager manager = (LocationManager) getSystemService(
            Context.LOCATION_SERVICE
        );
        String provider = availableProvider(manager);
        if (provider == null) {
            locationStatus.setText(R.string.location_services_disabled);
            return;
        }

        locationStatus.setText(R.string.location_finding);
        locationTimeout = () -> {
            cancelLocationRequest();
            useLastKnownLocationOrDefault(manager);
        };
        mainHandler.postDelayed(
            locationTimeout,
            LOCATION_TIMEOUT_MILLIS
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationCancellationSignal = new CancellationSignal();
            manager.getCurrentLocation(
                provider,
                locationCancellationSignal,
                getMainExecutor(),
                location -> handleFreshLocation(manager, location)
            );
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleFreshLocation(manager, location);
            }

            @Override
            public void onStatusChanged(
                String provider,
                int status,
                Bundle extras
            ) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onProviderDisabled(String provider) {
                handleFreshLocation(manager, null);
            }
        };
        manager.requestSingleUpdate(
            provider,
            locationListener,
            Looper.getMainLooper()
        );
    }

    private String availableProvider(LocationManager manager) {
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        return null;
    }

    private void handleFreshLocation(
        LocationManager manager,
        Location location
    ) {
        cancelLocationRequest();
        if (location == null) {
            useLastKnownLocationOrDefault(manager);
            return;
        }
        viewModel.updateLocation(
            location.getLatitude(),
            location.getLongitude()
        );
        locationStatus.setText(R.string.location_ready);
    }

    private void useLastKnownLocationOrDefault(LocationManager manager) {
        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            locationStatus.setText(R.string.location_default);
            return;
        }

        Location networkLocation = manager.getLastKnownLocation(
            LocationManager.NETWORK_PROVIDER
        );
        Location gpsLocation = manager.getLastKnownLocation(
            LocationManager.GPS_PROVIDER
        );
        Location location = newerLocation(networkLocation, gpsLocation);
        if (location == null) {
            locationStatus.setText(R.string.location_unavailable);
            return;
        }

        viewModel.updateLocation(
            location.getLatitude(),
            location.getLongitude()
        );
        locationStatus.setText(R.string.location_last_known);
    }

    private Location newerLocation(Location first, Location second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.getTime() >= second.getTime() ? first : second;
    }

    private void cancelLocationRequest() {
        if (locationTimeout != null) {
            mainHandler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }
        if (locationCancellationSignal != null) {
            locationCancellationSignal.cancel();
            locationCancellationSignal = null;
        }
        if (locationListener != null) {
            LocationManager manager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE
            );
            manager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    private void onDiscoverClicked() {
        if (needsLocalNetworkPermission()) {
            localNetworkPermissionLauncher.launch(
                Manifest.permission.ACCESS_LOCAL_NETWORK
            );
            return;
        }

        DiscoveryPreferences preferences = currentPreferences();
        preferencesStore.saveDiscoveryPreferences(preferences);
        RecommendationQuery query = new RecommendationQuery(
            viewModel.getLatitude(),
            viewModel.getLongitude(),
            selectedValue(categorySpinner, "Any category"),
            indoorSwitch.isChecked() ? Boolean.TRUE : null,
            walkingMinutes(),
            selectedValue(budgetSpinner, "Any budget"),
            textOf(atmosphereInput),
            openOnlySwitch.isChecked(),
            10
        );
        viewModel.loadRecommendations(query);
    }

    private DiscoveryPreferences currentPreferences() {
        String atmosphere = textOf(atmosphereInput);
        return new DiscoveryPreferences(
            categorySpinner.getSelectedItemPosition(),
            budgetSpinner.getSelectedItemPosition(),
            atmosphere == null ? "" : atmosphere,
            indoorSwitch.isChecked(),
            openOnlySwitch.isChecked(),
            walkingRangeSeekBar.getProgress()
        );
    }

    private boolean needsLocalNetworkPermission() {
        return BuildConfig.DEBUG
            && Build.VERSION.SDK_INT >= 37
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED;
    }

    private void renderState(DiscoveryUiState state) {
        switch (state.status()) {
            case IDLE -> setLoading(false);
            case LOADING -> setLoading(true);
            case SUCCESS -> {
                setLoading(false);
                adapter.submitList(state.recommendations());
                boolean empty = state.recommendations().isEmpty();
                resultsHeading.setVisibility(
                    empty ? View.GONE : View.VISIBLE
                );
                messageView.setText(R.string.no_results);
                messageView.setVisibility(
                    empty ? View.VISIBLE : View.GONE
                );
            }
            case ERROR -> {
                setLoading(false);
                adapter.submitList(List.of());
                resultsHeading.setVisibility(View.GONE);
                messageView.setText(getString(
                    R.string.recommendations_error,
                    state.errorMessage()
                ));
                messageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private String selectedValue(Spinner spinner, String emptyLabel) {
        String selected = spinner.getSelectedItem().toString();
        return selected.equals(emptyLabel)
            ? null
            : selected.toLowerCase(Locale.ROOT);
    }

    private String textOf(TextInputEditText input) {
        if (input.getText() == null) {
            return null;
        }
        String value = input.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private int walkingMinutes() {
        return walkingRangeSeekBar.getProgress() + 5;
    }

    private void updateWalkingRangeLabel() {
        walkingRangeLabel.setText(getResources().getQuantityString(
            R.plurals.walking_range,
            walkingMinutes(),
            walkingMinutes()
        ));
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        discoverButton.setEnabled(!loading);
        if (loading) {
            messageView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.refreshFavourites();
        }
    }

    @Override
    protected void onDestroy() {
        cancelLocationRequest();
        super.onDestroy();
    }
}
