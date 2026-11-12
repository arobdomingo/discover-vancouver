package com.discovervancouver.mobile.ui;

public final class LocationPermissionPolicy {

    public enum Action {
        REQUEST_PERMISSION,
        USE_DEVICE_LOCATION,
        KEEP_DOWNTOWN_FALLBACK
    }

    private LocationPermissionPolicy() { }

    public static Action beforeRequest(boolean alreadyGranted) {
        return alreadyGranted
            ? Action.USE_DEVICE_LOCATION
            : Action.REQUEST_PERMISSION;
    }

    public static Action afterRequest(boolean granted) {
        return granted
            ? Action.USE_DEVICE_LOCATION
            : Action.KEEP_DOWNTOWN_FALLBACK;
    }
}
