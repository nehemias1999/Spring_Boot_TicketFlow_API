package com.ticketflow.user_service.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating and validating JSON Web Tokens (JWT).
 * <p>
 * Uses the JJWT 0.12.x library with HMAC-SHA256 signing. The signing secret
 * and token expiration duration are injected from the application configuration.
 * </p>
 * <p>
 * JWT claims included in every token:
 * <ul>
 *   <li>{@code sub} — user ID</li>
 *   <li>{@code email} — user's email address</li>
 *   <li>{@code role} — user's role name (e.g., "USER", "ADMIN")</li>
 *   <li>{@code iat} — issued-at timestamp</li>
 *   <li>{@code exp} — expiration timestamp</li>
 * </ul>
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Constructs a {@code JwtUtil} instance with the configured secret and expiration.
     *
     * @param secret       the HMAC signing secret read from {@code jwt.secret}
     * @param expirationMs the token lifetime in milliseconds read from {@code jwt.expiration-ms}
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT token containing the user's core identity claims.
     *
     * @param userId the unique identifier of the user (used as {@code sub})
     * @param email  the user's email address (added as a custom claim)
     * @param role   the user's role name (added as a custom claim)
     * @return a compact, URL-safe signed JWT string
     */
    public String generateToken(String userId, String email, String role) {
        log.debug("Generating JWT token for userId: {}", userId);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and returns all claims from a given JWT token.
     *
     * @param token the compact JWT string to parse
     * @return the {@link Claims} object containing all token claims
     * @throws JwtException if the token is invalid, expired, or tampered with
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks whether a given JWT token is valid (correctly signed and not expired).
     *
     * @param token the compact JWT string to validate
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            log.debug("JWT token is valid");
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }
}
