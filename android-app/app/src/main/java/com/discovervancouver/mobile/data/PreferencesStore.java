package com.discovervancouver.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class PreferencesStore {

    private static final String FILE_NAME = "discovery_preferences";
    private static final String CATEGORY_POSITION = "category_position";
    private static final String BUDGET_POSITION = "budget_position";
    private static final String ATMOSPHERE = "atmosphere";
    private static final String INDOOR_PREFERRED = "indoor_preferred";
    private static final String OPEN_ONLY = "open_only";
    private static final String WALKING_RANGE = "walking_range";
    private static final String FAVOURITE_IDS = "favourite_ids";

    private final SharedPreferences preferences;

    public PreferencesStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        );
    }

    public DiscoveryPreferences loadDiscoveryPreferences() {
        DiscoveryPreferences defaults = DiscoveryPreferences.defaults();
        return new DiscoveryPreferences(
            preferences.getInt(
                CATEGORY_POSITION,
                defaults.categoryPosition()
            ),
            preferences.getInt(BUDGET_POSITION, defaults.budgetPosition()),
            preferences.getString(ATMOSPHERE, defaults.atmosphere()),
            preferences.getBoolean(
                INDOOR_PREFERRED,
                defaults.indoorPreferred()
            ),
            preferences.getBoolean(OPEN_ONLY, defaults.openOnly()),
            preferences.getInt(
                WALKING_RANGE,
                defaults.walkingRangeProgress()
            )
        );
    }

    public void saveDiscoveryPreferences(
        DiscoveryPreferences discoveryPreferences
    ) {
        preferences.edit()
            .putInt(
                CATEGORY_POSITION,
                discoveryPreferences.categoryPosition()
            )
            .putInt(BUDGET_POSITION, discoveryPreferences.budgetPosition())
            .putString(ATMOSPHERE, discoveryPreferences.atmosphere())
            .putBoolean(
                INDOOR_PREFERRED,
                discoveryPreferences.indoorPreferred()
            )
            .putBoolean(OPEN_ONLY, discoveryPreferences.openOnly())
            .putInt(
                WALKING_RANGE,
                discoveryPreferences.walkingRangeProgress()
            )
            .apply();
    }

    public boolean isFavourite(String placeId) {
        return preferences.getStringSet(FAVOURITE_IDS, Set.of())
            .contains(placeId);
    }

    public boolean toggleFavourite(String placeId) {
        Set<String> favourites = new HashSet<>(
            preferences.getStringSet(FAVOURITE_IDS, Set.of())
        );
        boolean nowFavourite;
        if (favourites.contains(placeId)) {
            favourites.remove(placeId);
            nowFavourite = false;
        } else {
            favourites.add(placeId);
            nowFavourite = true;
        }
        preferences.edit().putStringSet(FAVOURITE_IDS, favourites).apply();
        return nowFavourite;
    }
}
