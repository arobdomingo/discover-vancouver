package com.discovervancouver.importer;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.discovervancouver.config.PlaceSourceProperties;

@Component
public class VancouverOpenDataClient {

    private final RestClient restClient;
    private final int pageSize;

    public VancouverOpenDataClient(PlaceSourceProperties properties) {
        PlaceSourceProperties.Vancouver config = properties.getVancouver();
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .build();
        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(config.getReadTimeout());
        restClient = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
        pageSize = Math.max(1, Math.min(config.getPageSize(), 100));
    }

    public List<Map<String, Object>> fetchAll(String datasetId) {
        List<Map<String, Object>> records = new ArrayList<>();
        int offset = 0;
        long total;
        do {
            int pageOffset = offset;
            Map<?, ?> response = restClient.get()
                .uri(builder -> builder
                    .path("/api/explore/v2.1/catalog/datasets/{dataset}/records")
                    .queryParam("limit", pageSize)
                    .queryParam("offset", pageOffset)
                    .build(datasetId))
                .retrieve()
                .body(Map.class);
            if (response == null) {
                throw new IllegalStateException(
                    "Vancouver Open Data returned an empty response for "
                        + datasetId
                );
            }
            total = number(response.get("total_count"));
            Object results = response.get("results");
            if (!(results instanceof List<?> page)) {
                throw new IllegalStateException(
                    "Vancouver Open Data response has no results for "
                        + datasetId
                );
            }
            for (Object result : page) {
                if (result instanceof Map<?, ?> map) {
                    records.add(toStringKeyMap(map));
                }
            }
            offset += page.size();
            if (page.isEmpty()) break;
        } while (offset < total);
        return records;
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        java.util.LinkedHashMap<String, Object> result =
            new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key.toString(), value));
        return result;
    }
}
