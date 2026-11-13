package com.discovervancouver.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TestDatabaseGuardTest {
    @Test
    void acceptsOnlyTheDedicatedTestDatabase() {
        assertDoesNotThrow(() -> TestDatabaseGuard.requireIsolatedDatabase(
            "discover_vancouver_test"
        ));
        assertThrows(IllegalStateException.class, () ->
            TestDatabaseGuard.requireIsolatedDatabase("discover_vancouver")
        );
    }
}
