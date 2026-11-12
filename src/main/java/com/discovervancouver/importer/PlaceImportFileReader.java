package com.discovervancouver.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class PlaceImportFileReader {

    private static final List<String> REQUIRED_COLUMNS = List.of(
        "sourceid",
        "name",
        "category",
        "description",
        "address",
        "pricelevel",
        "indoor",
        "latitude",
        "longitude",
        "tags"
    );

    private final ObjectMapper objectMapper;

    public PlaceImportFileReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PlaceImportRecord> read(Path path) throws IOException {
        String fileName = path.getFileName().toString()
            .toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".json")) {
            return objectMapper.readValue(
                path.toFile(),
                new TypeReference<List<PlaceImportRecord>>() { }
            );
        }
        if (fileName.endsWith(".csv")) {
            return readCsv(path);
        }
        throw new IllegalArgumentException(
            "Import files must use a .json or .csv extension"
        );
    }

    private List<PlaceImportRecord> readCsv(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> columns = headerIndexes(rows.get(0));
        for (String required : REQUIRED_COLUMNS) {
            if (!columns.containsKey(required)) {
                throw new IllegalArgumentException(
                    "CSV is missing required column: " + required
                );
            }
        }

        List<PlaceImportRecord> records = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            try {
                records.add(new PlaceImportRecord(
                    value(row, columns, "sourceid"),
                    value(row, columns, "name"),
                    value(row, columns, "category"),
                    value(row, columns, "description"),
                    value(row, columns, "address"),
                    Integer.parseInt(value(row, columns, "pricelevel")),
                    parseBoolean(value(row, columns, "indoor")),
                    Map.of(),
                    Double.parseDouble(value(row, columns, "latitude")),
                    Double.parseDouble(value(row, columns, "longitude")),
                    parseTags(value(row, columns, "tags"))
                ));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(
                    "Invalid CSV row " + (rowIndex + 1) + ": "
                        + exception.getMessage(),
                    exception
                );
            }
        }
        return records;
    }

    private Map<String, Integer> headerIndexes(List<String> header) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            indexes.put(
                header.get(index).trim().toLowerCase(Locale.ROOT),
                index
            );
        }
        return indexes;
    }

    private String value(
        List<String> row,
        Map<String, Integer> columns,
        String column
    ) {
        int index = columns.get(column);
        return index < row.size() ? row.get(index).trim() : "";
    }

    private boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException(
            "indoor must be true or false"
        );
    }

    private List<String> parseTags(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|", -1)).stream()
            .map(String::trim)
            .toList();
    }

    static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < content.length()
                    && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                row.add(field.toString());
                field.setLength(0);
            } else if ((character == '\n' || character == '\r')
                && !quoted) {
                if (character == '\r' && index + 1 < content.length()
                    && content.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(character);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException(
                "CSV contains an unterminated quoted field"
            );
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
