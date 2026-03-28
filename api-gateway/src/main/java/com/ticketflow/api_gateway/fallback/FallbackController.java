package com.ticketflow.api_gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller invoked by the circuit breaker when a downstream service
 * is unavailable. Returns a consistent 503 response body instead of propagating
 * a raw connection error to the client.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/events")
    public Mono<ResponseEntity<Map<String, Object>>> eventsFallback() {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 503,
                "error", "Service Unavailable",
                "message", "The event service is temporarily unavailable. Please try again later.",
                "path", "/fallback/events"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }

    @GetMapping("/tickets")
    public Mono<ResponseEntity<Map<String, Object>>> ticketsFallback() {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 503,
                "error", "Service Unavailable",
                "message", "The ticket service is temporarily unavailable. Please try again later.",
                "path", "/fallback/tickets"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 503,
                "error", "Service Unavailable",
                "message", "The authentication service is temporarily unavailable. Please try again later.",
                "path", "/fallback/auth"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
