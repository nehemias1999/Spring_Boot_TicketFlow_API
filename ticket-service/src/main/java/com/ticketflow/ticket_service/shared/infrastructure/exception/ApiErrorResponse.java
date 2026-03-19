package com.ticketflow.ticket_service.shared.infrastructure.exception;

import java.time.LocalDateTime;

/**
 * Standard API error response returned by the global exception handler.
 * <p>
 * Provides a consistent error structure across all endpoints,
 * including timestamp, HTTP status, error type, message, request path,
 * and correlation ID for distributed tracing.
 * </p>
 *
 * @param timestamp     the date and time when the error occurred
 * @param status        the HTTP status code
 * @param error         the HTTP status reason phrase (e.g., "Not Found")
 * @param message       a human-readable description of the error
 * @param path          the request URI that triggered the error
 * @param correlationId the X-Correlation-Id header value for tracing
 * @author TicketFlow Team
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}
