package com.discovervancouver.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.discovervancouver.config.RateLimitProperties;

import tools.jackson.databind.ObjectMapper;

class RequestProtectionFilterTest {

    @Test
    void requestIdIsPreservedAndReturned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/health"
        );
        request.addHeader(RequestIdFilter.HEADER, "mobile-request-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(
            request,
            response,
            new MockFilterChain()
        );

        assertThat(response.getHeader(RequestIdFilter.HEADER))
            .isEqualTo("mobile-request-42");
        assertThat(RequestIdFilter.from(request))
            .isEqualTo("mobile-request-42");
    }

    @Test
    void rateLimitRejectsRequestsBeyondConfiguredWindow() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequestsPerMinute(2);
        RateLimitFilter filter = new RateLimitFilter(
            properties,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC)
        );
        AtomicInteger accepted = new AtomicInteger();

        MockHttpServletResponse first = invoke(filter, accepted);
        MockHttpServletResponse second = invoke(filter, accepted);
        MockHttpServletResponse third = invoke(filter, accepted);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isEqualTo("60");
        assertThat(third.getContentAsString())
            .contains("Request rate limit exceeded");
        assertThat(accepted).hasValue(2);
    }

    private MockHttpServletResponse invoke(
        RateLimitFilter filter,
        AtomicInteger accepted
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/recommendations"
        );
        request.setRemoteAddr("192.0.2.10");
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "test-request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(
            request,
            response,
            (ignoredRequest, ignoredResponse) -> accepted.incrementAndGet()
        );
        return response;
    }
}
