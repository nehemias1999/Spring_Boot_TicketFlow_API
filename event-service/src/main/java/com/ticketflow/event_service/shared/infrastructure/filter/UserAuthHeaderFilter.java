package com.ticketflow.event_service.shared.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Security filter applied to every inbound request on the event-service.
 *
 * <p>Three path categories are distinguished:
 * <ol>
 *   <li><b>Actuator</b> ({@code /actuator/**}) — always allowed, no header required.</li>
 *   <li><b>Internal</b> (capacity endpoints, seller-event-ids) — must carry a valid
 *       {@code X-Internal-Api-Key} header matching the configured secret.</li>
 *   <li><b>Public reads</b> ({@code GET /api/v1/events} and {@code GET /api/v1/events/{id}})
 *       — no auth required.</li>
 *   <li><b>All other paths</b> — must carry a non-blank {@code X-User-Id} header.</li>
 * </ol>
 */
@Component
public class UserAuthHeaderFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER      = "X-User-Id";
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";

    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public UserAuthHeaderFilter(
            ObjectMapper objectMapper,
            @Value("${services.internal-api-key}") String internalApiKey) {
        this.objectMapper  = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // 1. Actuator — always pass through
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Internal service-to-service endpoints — require valid internal API key
        if (isInternalPath(path)) {
            String key = request.getHeader(INTERNAL_KEY_HEADER);
            if (!internalApiKey.equals(key)) {
                writeUnauthorized(response, "Missing or invalid header: " + INTERNAL_KEY_HEADER);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Public read endpoints — no auth required
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/events(/[^/]+)?")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. All other endpoints — require X-User-Id
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            writeUnauthorized(response, "Missing required header: " + USER_ID_HEADER);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalPath(String path) {
        return path.matches("/api/v1/events/.+/decrement-tickets")
                || path.matches("/api/v1/events/.+/increment-tickets")
                || path.equals("/api/v1/events/my-event-ids");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status",    401,
                "error",     "Unauthorized",
                "message",   message
        ));
    }
}
