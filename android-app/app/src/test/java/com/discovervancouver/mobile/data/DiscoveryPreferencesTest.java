package com.discovervancouver.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DiscoveryPreferencesTest {

    @Test
    public void defaultsRepresentUnfilteredDowntownSearch() {
        DiscoveryPreferences preferences = DiscoveryPreferences.defaults();

        assertEquals(0, preferences.categoryPosition());
        assertEquals(0, preferences.budgetPosition());
        assertEquals("", preferences.atmosphere());
        assertFalse(preferences.indoorPreferred());
        assertFalse(preferences.openOnly());
        assertEquals(15, preferences.walkingRangeProgress());
    }
}
