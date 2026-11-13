package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FoursquareCategoryMapperTest {

    @Test
    void categoryBreadcrumbTagsFitThePlaceValidationLimit() {
        FoursquareCategoryMapper.MappedCategory mapped =
            new FoursquareCategoryMapper().map(List.of(
                "Business and Professional Services > Advertising Agencies > Specialized Marketing Services"
            ));

        assertTrue(mapped.tags().stream()
            .allMatch(tag -> tag.length() <= 50));
    }
}
