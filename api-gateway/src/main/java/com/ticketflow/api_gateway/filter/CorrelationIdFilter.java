package com.ticketflow.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Global filter that ensures every request carries an {@code X-Correlation-Id} header.
 * <p>
 * If the incoming request already contains the header, its value is preserved and
 * forwarded downstream. Otherwise, a new UUID is generated. The correlation ID is also
 * added to the outgoing response so the client can trace the request end-to-end.
 * </p>
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = Optional.ofNullable(request.getHeaders().getFirst(CORRELATION_ID_HEADER))
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());

        log.info("[{}] {} {}", correlationId, request.getMethod(), request.getURI().getPath());

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, correlationId))
                .build();

        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
