package com.discovervancouver.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String requestId,
    Map<String, List<String>> errors
) {

}
