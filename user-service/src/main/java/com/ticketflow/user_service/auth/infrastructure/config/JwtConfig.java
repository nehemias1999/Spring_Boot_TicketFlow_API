package com.ticketflow.user_service.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token generation.
 * <p>
 * Values are read from {@code application.yml} (or the config server)
 * under the {@code jwt} prefix. They are injected directly into
 * {@link com.ticketflow.user_service.shared.util.JwtUtil} via
 * {@code @Value} annotations.
 * </p>
 * <p>
 * This class documents the expected JWT configuration keys:
 * <ul>
 *   <li>{@code jwt.secret} — HMAC-SHA256 signing key (minimum 256 bits / 32 chars recommended)</li>
 *   <li>{@code jwt.expiration-ms} — token lifetime in milliseconds (e.g., 86400000 = 24 h)</li>
 * </ul>
 * </p>
 *
 * @author TicketFlow Team
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(String secret, long expirationMs) {
}
