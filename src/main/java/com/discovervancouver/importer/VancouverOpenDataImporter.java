package com.discovervancouver.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VancouverOpenDataImporter {

    private static final Logger logger =
        LoggerFactory.getLogger(VancouverOpenDataImporter.class);

    private final VancouverOpenDataClient client;
    private final VancouverOpenDataMapper mapper;
    private final PlaceImportService importService;

    public VancouverOpenDataImporter(
        VancouverOpenDataClient client,
        VancouverOpenDataMapper mapper,
        PlaceImportService importService
    ) {
        this.client = client;
        this.mapper = mapper;
        this.importService = importService;
    }

    public VancouverImportResult importAll() {
        Map<String, List<String>> parkFacilities;
        try {
            parkFacilities = parkFacilities();
        } catch (RuntimeException exception) {
            logger.warn(
                "Park facility enrichment is unavailable; importing parks without facility tags",
                exception
            );
            parkFacilities = Map.of();
        }
        Map<VancouverDataset, PlaceImportReport> reports = new HashMap<>();
        Map<VancouverDataset, String> failures = new HashMap<>();
        for (VancouverDataset dataset : VancouverDataset.values()) {
            try {
                List<PlaceImportRecord> records = new ArrayList<>();
                List<Map<String, Object>> rawRecords = newestSnapshot(
                    dataset,
                    client.fetchAll(dataset.id())
                );
                for (Map<String, Object> raw : rawRecords) {
                    List<String> relatedTags = dataset == VancouverDataset.PARKS
                        ? parkFacilities.getOrDefault(
                            mapper.sourceId(raw),
                            List.of()
                        )
                        : List.of();
                    mapper.map(dataset, raw, relatedTags).ifPresent(records::add);
                }
                reports.put(
                    dataset,
                    importService.importSnapshot(
                        "vancouver-" + dataset.id(),
                        records
                    )
                );
            } catch (RuntimeException exception) {
                failures.put(dataset, exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage());
                logger.error(
                    "Vancouver dataset refresh failed dataset={}",
                    dataset.id(),
                    exception
                );
            }
        }
        return new VancouverImportResult(reports, failures);
    }

    private List<Map<String, Object>> newestSnapshot(
        VancouverDataset dataset,
        List<Map<String, Object>> records
    ) {
        if (dataset != VancouverDataset.CULTURAL_SPACES) return records;
        int latestYear = records.stream()
            .map(record -> record.get("year"))
            .mapToInt(this::year)
            .max()
            .orElse(Integer.MIN_VALUE);
        if (latestYear == Integer.MIN_VALUE) return records;
        return records.stream()
            .filter(record -> year(record.get("year")) == latestYear)
            .toList();
    }

    private int year(Object value) {
        if (value == null) return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private Map<String, List<String>> parkFacilities() {
        Map<String, List<String>> facilities = new HashMap<>();
        for (Map<String, Object> raw : client.fetchAll("parks-facilities")) {
            String parkId = mapper.sourceId(raw);
            String tag = mapper.facilityTag(raw);
            if (!parkId.isBlank() && !tag.isBlank()) {
                facilities.computeIfAbsent(parkId, ignored -> new ArrayList<>())
                    .add(tag);
            }
        }
        return facilities;
    }
}
