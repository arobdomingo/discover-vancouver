package com.discovervancouver.mobile.data;

public record DiscoveryPreferences(
    int categoryPosition,
    int budgetPosition,
    String atmosphere,
    boolean indoorPreferred,
    boolean openOnly,
    int walkingRangeProgress
) {
    public static DiscoveryPreferences defaults() {
        return new DiscoveryPreferences(0, 0, "", false, false, 15);
    }
}
