package com.discovervancouver.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.discovervancouver.config.RateLimitProperties;
import com.discovervancouver.dto.ApiErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows =
        new ConcurrentHashMap<>();

    public RateLimitFilter(
        RateLimitProperties properties,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long now = Instant.now(clock).getEpochSecond();
        long windowStart = now - (now % WINDOW_SECONDS);
        String client = clientAddress(request);
        Window window = windows.compute(client, (ignored, current) -> {
            if (current == null || current.startedAt() != windowStart) {
                return new Window(windowStart, 1);
            }
            return new Window(windowStart, current.requests() + 1);
        });
        int limit = properties.getRequestsPerMinute();
        response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
        response.setHeader(
            "X-RateLimit-Remaining",
            Integer.toString(Math.max(0, limit - window.requests()))
        );

        if (window.requests() <= limit) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(
            "Retry-After",
            Long.toString(WINDOW_SECONDS - (now - windowStart))
        );
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
            Instant.now(clock),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            "Request rate limit exceeded",
            request.getRequestURI(),
            RequestIdFilter.from(request),
            Map.of("rateLimit", List.of(
                "Try again after the Retry-After interval"
            ))
        ));
    }

    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(long startedAt, int requests) {
    }
}
