package com.discovervancouver.support;

import org.springframework.data.mongodb.core.MongoTemplate;

public final class TestDatabaseGuard {
    private TestDatabaseGuard() {
    }

    public static void requireIsolatedDatabase(MongoTemplate mongoTemplate) {
        requireIsolatedDatabase(mongoTemplate.getDb().getName());
    }

    static void requireIsolatedDatabase(String databaseName) {
        if (!"discover_vancouver_test".equals(databaseName)) {
            throw new IllegalStateException(
                "Refusing destructive test cleanup against database: "
                    + databaseName
            );
        }
    }
}
