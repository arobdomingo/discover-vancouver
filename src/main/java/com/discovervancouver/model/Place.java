package com.discovervancouver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Document(collection = "places")
@CompoundIndex(
    name = "source_identity_unique",
    def = "{'source': 1, 'sourceId': 1}",
    sparse = true,
    unique = true
)
public class Place {
    
    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @NotBlank
    private String description;

    @NotBlank
    private String address;

    @Min(0)
    @Max(3)
    private int priceLevel;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double latitude;
    
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private double longitude;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String source;

    private String sourceId;

    private Instant importedAt;

    private String sourceUrl;

    private Instant lastRefreshedAt;

    private Instant lastSeenAt;

    private PlaceOperatingStatus operatingStatus =
        PlaceOperatingStatus.UNKNOWN;

    private DataQuality dataQuality = DataQuality.UNKNOWN;

    private boolean stale;

    private String canonicalPlaceId;

    private boolean priceKnown = true;

    private boolean indoorKnown = true;

    private Map<String, String> fieldSources = Map.of();

    @NotNull
    private List<String> tags;


    private boolean indoor;

    @NotNull
    private Map<DayOfWeek, List<OpeningPeriod>> openingHours;

    public Place(String id, String name, String category, String description, String address, int priceLevel, boolean indoor, Map<DayOfWeek, List<OpeningPeriod>> openingHours, double latitude, double longitude, List<String> tags) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.address = address;
        this.priceLevel = priceLevel;
        this.indoor = indoor;
        this.openingHours = openingHours;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = new GeoJsonPoint(longitude, latitude);
        this.tags = tags;
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public int getPriceLevel() {
        return priceLevel;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }

    public GeoJsonPoint getLocation() {
        return location;
    }

    public void synchronizeLocation() {
        this.location = new GeoJsonPoint(longitude, latitude);
    }

    public String getSource() {
        return source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportMetadata(
        String source,
        String sourceId,
        Instant importedAt
    ) {
        this.source = source;
        this.sourceId = sourceId;
        this.importedAt = importedAt;
    }

    public void setImportMetadata(
        String source,
        String sourceId,
        String sourceUrl,
        Instant importedAt,
        Instant lastRefreshedAt,
        Instant lastSeenAt,
        PlaceOperatingStatus operatingStatus,
        DataQuality dataQuality,
        boolean stale,
        boolean priceKnown,
        boolean indoorKnown,
        Map<String, String> fieldSources
    ) {
        setImportMetadata(source, sourceId, importedAt);
        this.sourceUrl = sourceUrl;
        this.lastRefreshedAt = lastRefreshedAt;
        this.lastSeenAt = lastSeenAt;
        this.operatingStatus = operatingStatus == null
            ? PlaceOperatingStatus.UNKNOWN
            : operatingStatus;
        this.dataQuality = dataQuality == null
            ? DataQuality.UNKNOWN
            : dataQuality;
        this.stale = stale;
        this.priceKnown = priceKnown;
        this.indoorKnown = indoorKnown;
        this.fieldSources = fieldSources == null
            ? Map.of()
            : Map.copyOf(fieldSources);
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public PlaceOperatingStatus getOperatingStatus() {
        return operatingStatus;
    }

    public DataQuality getDataQuality() {
        return dataQuality;
    }

    public boolean isStale() {
        return stale;
    }

    public void markStale() {
        stale = true;
    }

    public void markActive(Instant seenAt) {
        stale = false;
        lastSeenAt = seenAt;
    }

    public boolean isDiscoverable() {
        return !stale
            && operatingStatus != PlaceOperatingStatus.PERMANENTLY_CLOSED
            && operatingStatus != PlaceOperatingStatus.TEMPORARILY_CLOSED;
    }

    public String getCanonicalPlaceId() {
        return canonicalPlaceId;
    }

    public void setCanonicalPlaceId(String canonicalPlaceId) {
        this.canonicalPlaceId = canonicalPlaceId;
    }

    public void ensureCanonicalPlaceId() {
        if (canonicalPlaceId == null || canonicalPlaceId.isBlank()) {
            canonicalPlaceId = UUID.randomUUID().toString();
        }
    }

    public boolean isPriceKnown() {
        return priceKnown;
    }

    public boolean isIndoorKnown() {
        return indoorKnown;
    }

    public boolean isHoursKnown() {
        return openingHours != null && !openingHours.isEmpty();
    }

    public Map<String, String> getFieldSources() {
        return fieldSources;
    }

    public List<String> getTags(){
        return tags;
    }

    public Map<DayOfWeek, List<OpeningPeriod>> getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(
        Map<DayOfWeek, List<OpeningPeriod>> openingHours
    ) {
        this.openingHours = openingHours;
    }

}
