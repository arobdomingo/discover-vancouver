package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VancouverOpenDataImporterTest {

    @Test
    void oneDatasetAndOptionalEnrichmentFailureDoNotBlockOtherImports() {
        VancouverOpenDataClient client = mock(VancouverOpenDataClient.class);
        VancouverOpenDataMapper mapper = mock(VancouverOpenDataMapper.class);
        PlaceImportService importService = mock(PlaceImportService.class);
        PlaceImportReport report = new PlaceImportReport(
            0, 0, 0, 0, 0, List.of()
        );

        when(client.fetchAll("parks-facilities"))
            .thenThrow(new RuntimeException("temporary enrichment outage"));
        when(client.fetchAll(VancouverDataset.PARKS.id()))
            .thenThrow(new RuntimeException("temporary parks outage"));
        for (VancouverDataset dataset : VancouverDataset.values()) {
            if (dataset != VancouverDataset.PARKS) {
                when(client.fetchAll(dataset.id())).thenReturn(List.of());
                when(importService.importSnapshot(
                    eq("vancouver-" + dataset.id()),
                    anyList()
                )).thenReturn(report);
            }
        }

        VancouverOpenDataImporter importer = new VancouverOpenDataImporter(
            client,
            mapper,
            importService
        );

        VancouverImportResult result = importer.importAll();

        assertEquals(VancouverDataset.values().length - 1, result.reports().size());
        assertEquals(1, result.failures().size());
        verify(importService, never()).importSnapshot(
            eq("vancouver-parks"),
            anyList()
        );
        for (VancouverDataset dataset : VancouverDataset.values()) {
            if (dataset != VancouverDataset.PARKS) {
                verify(importService).importSnapshot(
                    eq("vancouver-" + dataset.id()),
                    anyList()
                );
            }
        }
    }

    @Test
    void importsOnlyTheNewestCulturalSpacesSnapshot() {
        VancouverOpenDataClient client = mock(VancouverOpenDataClient.class);
        PlaceImportService importService = mock(PlaceImportService.class);
        PlaceImportReport report = new PlaceImportReport(
            0, 0, 0, 0, 0, List.of()
        );
        when(client.fetchAll("parks-facilities")).thenReturn(List.of());
        when(client.fetchAll(VancouverDataset.CULTURAL_SPACES.id()))
            .thenReturn(List.of(
                culturalSpace("Historical Space", "2017"),
                culturalSpace("Current Space", "2020")
            ));
        when(importService.importSnapshot(anyString(), anyList()))
            .thenReturn(report);

        VancouverOpenDataImporter importer = new VancouverOpenDataImporter(
            client,
            new VancouverOpenDataMapper(),
            importService
        );
        importer.importAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlaceImportRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(importService).importSnapshot(
            eq("vancouver-cultural-spaces"),
            captor.capture()
        );
        assertEquals(1, captor.getValue().size());
        assertEquals("Current Space", captor.getValue().getFirst().name());
    }

    private Map<String, Object> culturalSpace(String name, String year) {
        return Map.of(
            "year", year,
            "cultural_space_name", name,
            "address", "123 Example St, Vancouver, BC",
            "geo_point_2d", Map.of("lat", 49.28, "lon", -123.12)
        );
    }
}
