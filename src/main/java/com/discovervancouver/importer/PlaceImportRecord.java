package com.discovervancouver.importer;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import com.discovervancouver.model.OpeningPeriod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceImportRecord(
    @NotBlank
    @Size(max = 120)
    String sourceId,

    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Size(max = 50)
    String category,

    @NotBlank
    @Size(max = 1_000)
    String description,

    @NotBlank
    @Size(max = 250)
    String address,

    @Min(0)
    @Max(3)
    int priceLevel,

    boolean indoor,

    Map<DayOfWeek, List<@Valid OpeningPeriod>> openingHours,

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    double latitude,

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    double longitude,

    @NotNull
    @Size(max = 20)
    List<@NotBlank @Size(max = 50) String> tags
) {
}
