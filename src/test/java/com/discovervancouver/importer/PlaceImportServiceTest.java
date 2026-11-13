package com.discovervancouver.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.discovervancouver.model.Place;
import com.discovervancouver.repository.PlaceRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PlaceImportServiceTest {

    private final PlaceRepository repository = mock(PlaceRepository.class);
    private final Validator validator = Validation
        .buildDefaultValidatorFactory()
        .getValidator();
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-11-12T18:00:00Z"),
        ZoneOffset.UTC
    );
    private final PlaceImportService service = new PlaceImportService(
        repository,
        validator,
        clock
    );

    @Test
    void normalizesAndCreatesValidRecordWithProvenance() {
        when(repository.findBySourceAndSourceId("city-data", "cafe-1"))
            .thenReturn(Optional.empty());

        PlaceImportReport report = service.importRecords(
            " City-Data ",
            List.of(validRecord(" Cafe-1 ", " CAFE "))
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Place>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        Place saved = captor.getValue().getFirst();
        assertEquals(1, report.created());
        assertEquals(0, report.updated());
        assertEquals("cafe", saved.getCategory());
        assertEquals(List.of("quiet", "wifi"), saved.getTags());
        assertEquals("city-data", saved.getSource());
        assertEquals("cafe-1", saved.getSourceId());
        assertNotNull(saved.getLocation());
        assertEquals(Instant.now(clock), saved.getImportedAt());
    }

    @Test
    void sameSourceIdentityUpdatesInsteadOfDuplicating() {
        Place existing = validPlace("mongo-id");
        when(repository.findBySourceAndSourceId("city-data", "cafe-1"))
            .thenReturn(Optional.of(existing));

        PlaceImportReport report = service.importRecords(
            "city-data",
            List.of(validRecord("cafe-1", "cafe"))
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Place>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals("mongo-id", captor.getValue().getFirst().getId());
        assertEquals(0, report.created());
        assertEquals(1, report.updated());
    }

    @Test
    void rejectsInvalidRecordWithoutSavingIt() {
        PlaceImportRecord invalid = validRecord("cafe-1", " ");

        PlaceImportReport report = service.importRecords(
            "city-data",
            List.of(invalid)
        );

        assertEquals(1, report.rejected());
        assertEquals(1, report.errors().size());
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void completeSnapshotMarksMissingSourceRecordsStale() {
        Place missing = validPlace("missing-id");
        missing.setImportMetadata(
            "city-data",
            "missing-source-id",
            Instant.parse("2026-11-01T00:00:00Z")
        );
        when(repository.findBySourceAndSourceId("city-data", "cafe-1"))
            .thenReturn(Optional.empty());
        when(repository.findAllBySource("city-data"))
            .thenReturn(List.of(missing));

        PlaceImportReport report = service.importSnapshot(
            "city-data",
            List.of(validRecord("cafe-1", "cafe"))
        );

        assertEquals(1, report.staleMarked());
        assertEquals(true, missing.isStale());
    }

    private PlaceImportRecord validRecord(
        String sourceId,
        String category
    ) {
        return new PlaceImportRecord(
            sourceId,
            "Example Cafe",
            category,
            "A calm cafe.",
            "123 Example St, Vancouver, BC",
            1,
            true,
            Map.of(),
            49.2827,
            -123.1207,
            List.of(" Quiet ", "wifi", "quiet")
        );
    }

    private Place validPlace(String id) {
        return new Place(
            id,
            "Old Cafe",
            "cafe",
            "Old data.",
            "123 Example St, Vancouver, BC",
            1,
            true,
            Map.of(),
            49.2827,
            -123.1207,
            List.of("quiet")
        );
    }
}
