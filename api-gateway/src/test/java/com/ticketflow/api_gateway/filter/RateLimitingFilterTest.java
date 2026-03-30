package com.ticketflow.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RateLimitingFilter}.
 */
class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        // Allow only 3 requests per 60 seconds for testing
        filter = new RateLimitingFilter(3, 60_000);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldAllowRequestsBelowLimit() {
        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/events")
                    .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 1234))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .as("Request %d should be allowed", i + 1)
                    .isNull();
        }
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() {
        // Exhaust the limit (3 requests)
        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/events")
                    .remoteAddress(new java.net.InetSocketAddress("10.0.0.2", 1234))
                    .build();
            filter.filter(MockServerWebExchange.from(request), chain).block();
        }

        // 4th request should be rate-limited
        MockServerHttpRequest blockedRequest = MockServerHttpRequest
                .get("/api/v1/events")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.2", 1234))
                .build();
        MockServerWebExchange blockedExchange = MockServerWebExchange.from(blockedRequest);

        StepVerifier.create(filter.filter(blockedExchange, chain))
                .verifyComplete();

        assertThat(blockedExchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldTrackRateLimitPerIp() {
        // Exhaust limit for IP 10.0.0.3
        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/events")
                    .remoteAddress(new java.net.InetSocketAddress("10.0.0.3", 1234))
                    .build();
            filter.filter(MockServerWebExchange.from(request), chain).block();
        }

        // Different IP should still be allowed
        MockServerHttpRequest otherIpRequest = MockServerHttpRequest
                .get("/api/v1/events")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.4", 1234))
                .build();
        MockServerWebExchange otherExchange = MockServerWebExchange.from(otherIpRequest);

        StepVerifier.create(filter.filter(otherExchange, chain))
                .verifyComplete();

        assertThat(otherExchange.getResponse().getStatusCode())
                .as("Different IP should not be rate limited")
                .isNull();
    }

    @Test
    void shouldRespectXForwardedForHeader() {
        // Exhaust limit for forwarded IP
        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/events")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            filter.filter(MockServerWebExchange.from(request), chain).block();
        }

        // 4th request with same forwarded IP
        MockServerHttpRequest blockedRequest = MockServerHttpRequest
                .get("/api/v1/events")
                .header("X-Forwarded-For", "192.168.1.1")
                .build();
        MockServerWebExchange blockedExchange = MockServerWebExchange.from(blockedRequest);

        StepVerifier.create(filter.filter(blockedExchange, chain))
                .verifyComplete();

        assertThat(blockedExchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
