package com.ticketflow.event_service.shared.infrastructure.exception;

import java.time.LocalDateTime;

/**
 * Standard API error response returned by the global exception handler.
 * <p>
 * Provides a consistent error structure across all endpoints,
 * including timestamp, HTTP status, error type, message, and request path.
 * </p>
 *
 * @param timestamp the date and time when the error occurred
 * @param status    the HTTP status code
 * @param error     the HTTP status reason phrase (e.g., "Not Found")
 * @param message   a human-readable description of the error
 * @param path      the request URI that triggered the error
 * @author TicketFlow Team
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}

