package com.discovervancouver.importer;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.model.Place;
import com.discovervancouver.model.DataQuality;
import com.discovervancouver.model.PlaceOperatingStatus;
import com.discovervancouver.repository.PlaceRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class PlaceImportService {

    private final PlaceRepository placeRepository;
    private final Validator validator;
    private final Clock clock;
    private final PlaceDeduplicationService deduplicationService;

    @Autowired
    public PlaceImportService(
        PlaceRepository placeRepository,
        Validator validator,
        Clock clock,
        PlaceDeduplicationService deduplicationService
    ) {
        this.placeRepository = placeRepository;
        this.validator = validator;
        this.clock = clock;
        this.deduplicationService = deduplicationService;
    }

    public PlaceImportService(
        PlaceRepository placeRepository,
        Validator validator,
        Clock clock
    ) {
        this(
            placeRepository,
            validator,
            clock,
            new PlaceDeduplicationService(placeRepository)
        );
    }

    public PlaceImportReport importRecords(
        String source,
        List<PlaceImportRecord> records
    ) {
        return importRecords(source, records, false);
    }

    public PlaceImportReport importSnapshot(
        String source,
        List<PlaceImportRecord> records
    ) {
        return importRecords(source, records, true);
    }

    private PlaceImportReport importRecords(
        String source,
        List<PlaceImportRecord> records,
        boolean completeSnapshot
    ) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                "An import source name is required"
            );
        }

        int created = 0;
        int updated = 0;
        int duplicatesLinked = 0;
        List<String> errors = new ArrayList<>();
        String normalizedSource = normalizeIdentifier(source);
        Set<String> seenSourceIds = new HashSet<>();
        Instant importTime = Instant.now(clock);

        for (int index = 0; index < records.size(); index++) {
            PlaceImportRecord record = records.get(index);
            List<String> recordErrors = validator.validate(record).stream()
                .map(this::formatViolation)
                .sorted()
                .toList();

            if (!recordErrors.isEmpty()) {
                errors.add(
                    "Record " + (index + 1) + ": "
                        + String.join(", ", recordErrors)
                );
                continue;
            }

            String sourceId = normalizeIdentifier(record.sourceId());
            if (!seenSourceIds.add(sourceId)) {
                errors.add(
                    "Record " + (index + 1)
                        + ": duplicate sourceId " + sourceId
                );
                continue;
            }
            Place existing = placeRepository
                .findBySourceAndSourceId(normalizedSource, sourceId)
                .orElse(null);
            Place imported = toPlace(
                record,
                existing,
                normalizedSource,
                importTime
            );
            PlaceDeduplicationService.MatchResult match =
                deduplicationService.canonicalize(imported, existing);
            imported.setCanonicalPlaceId(match.canonicalPlaceId());
            placeRepository.save(imported);

            if (match.duplicateLinked()) {
                duplicatesLinked++;
            }

            if (existing == null) {
                created++;
            } else {
                updated++;
            }
        }

        int staleMarked = 0;
        if (completeSnapshot) {
            for (Place existing : placeRepository.findAllBySource(normalizedSource)) {
                if (!seenSourceIds.contains(existing.getSourceId())
                    && !existing.isStale()) {
                    existing.markStale();
                    placeRepository.save(existing);
                    staleMarked++;
                }
            }
        }

        return new PlaceImportReport(
            created,
            updated,
            errors.size(),
            staleMarked,
            duplicatesLinked,
            List.copyOf(errors)
        );
    }

    private Place toPlace(
        PlaceImportRecord record,
        Place existing,
        String source,
        Instant importTime
    ) {
        String sourceId = normalizeIdentifier(record.sourceId());
        Map<DayOfWeek, List<OpeningPeriod>> hours =
            record.openingHours() == null
                ? Map.of()
                : record.openingHours();
        List<String> normalizedTags = new ArrayList<>(
            new LinkedHashSet<>(record.tags().stream()
                .map(this::normalizeLabel)
                .filter(tag -> !tag.isBlank())
                .toList())
        );

        Place place = new Place(
            existing == null ? null : existing.getId(),
            record.name().trim(),
            normalizeLabel(record.category()),
            record.description().trim(),
            record.address().trim(),
            record.priceLevel(),
            record.indoor(),
            hours,
            record.latitude(),
            record.longitude(),
            normalizedTags
        );
        Instant importedAt = existing == null || existing.getImportedAt() == null
            ? importTime
            : existing.getImportedAt();
        place.setImportMetadata(
            source,
            sourceId,
            record.sourceUrl(),
            importedAt,
            record.lastRefreshedAt(),
            importTime,
            record.operatingStatus() == null
                ? PlaceOperatingStatus.UNKNOWN
                : record.operatingStatus(),
            record.dataQuality() == null
                ? DataQuality.UNKNOWN
                : record.dataQuality(),
            false,
            record.priceKnown() == null || record.priceKnown(),
            record.indoorKnown() == null || record.indoorKnown(),
            record.fieldSources()
        );
        return place;
    }

    private String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLabel(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatViolation(
        ConstraintViolation<PlaceImportRecord> violation
    ) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }
}
