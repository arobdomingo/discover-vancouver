package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocationPermissionPolicyTest {

    @Test
    public void deniedPermissionKeepsDowntownFallback() {
        assertEquals(
            LocationPermissionPolicy.Action.KEEP_DOWNTOWN_FALLBACK,
            LocationPermissionPolicy.afterRequest(false)
        );
    }

    @Test
    public void missingPermissionRequestsAccess() {
        assertEquals(
            LocationPermissionPolicy.Action.REQUEST_PERMISSION,
            LocationPermissionPolicy.beforeRequest(false)
        );
    }
}
