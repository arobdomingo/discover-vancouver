package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        Map<VancouverDataset, PlaceImportReport> reports = importer.importAll();

        assertEquals(VancouverDataset.values().length - 1, reports.size());
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
}
