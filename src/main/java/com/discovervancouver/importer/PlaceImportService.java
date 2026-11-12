package com.discovervancouver.importer;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.discovervancouver.model.OpeningPeriod;
import com.discovervancouver.model.Place;
import com.discovervancouver.repository.PlaceRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class PlaceImportService {

    private final PlaceRepository placeRepository;
    private final Validator validator;
    private final Clock clock;

    public PlaceImportService(
        PlaceRepository placeRepository,
        Validator validator,
        Clock clock
    ) {
        this.placeRepository = placeRepository;
        this.validator = validator;
        this.clock = clock;
    }

    public PlaceImportReport importRecords(
        String source,
        List<PlaceImportRecord> records
    ) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                "An import source name is required"
            );
        }

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        String normalizedSource = normalizeIdentifier(source);

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
            Place existing = placeRepository
                .findBySourceAndSourceId(normalizedSource, sourceId)
                .orElse(null);
            Place imported = toPlace(record, existing, normalizedSource);
            placeRepository.save(imported);

            if (existing == null) {
                created++;
            } else {
                updated++;
            }
        }

        return new PlaceImportReport(
            created,
            updated,
            errors.size(),
            List.copyOf(errors)
        );
    }

    private Place toPlace(
        PlaceImportRecord record,
        Place existing,
        String source
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
        Instant importedAt = Instant.now(clock);
        place.setImportMetadata(source, sourceId, importedAt);
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
