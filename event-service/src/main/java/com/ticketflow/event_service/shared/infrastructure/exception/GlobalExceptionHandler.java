package com.ticketflow.event_service.shared.infrastructure.exception;

import com.ticketflow.event_service.catalog.domain.exception.CatalogAlreadyExistsException;
import com.ticketflow.event_service.catalog.domain.exception.CatalogNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler that intercepts exceptions thrown by controllers
 * and returns standardized {@link ApiErrorResponse} objects.
 * <p>
 * Handles domain-specific exceptions (not found, already exists),
 * validation errors, and unexpected server errors.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link CatalogNotFoundException} when a requested catalog is not found.
     *
     * @param ex      the exception thrown
     * @param request the HTTP request that triggered the error
     * @return a 404 Not Found response with error details
     */
    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCatalogNotFoundException(
            CatalogNotFoundException ex, HttpServletRequest request) {
        log.warn("Catalog not found on request to '{}': {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles {@link CatalogAlreadyExistsException} when attempting to create
     * a catalog with a duplicate ID.
     *
     * @param ex      the exception thrown
     * @param request the HTTP request that triggered the error
     * @return a 409 Conflict response with error details
     */
    @ExceptionHandler(CatalogAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCatalogAlreadyExistsException(
            CatalogAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Catalog conflict on request to '{}': {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles bean validation errors triggered by {@code @Valid} annotations.
     * <p>
     * Collects all field-level error messages and joins them into a single
     * comma-separated string.
     * </p>
     *
     * @param ex      the validation exception containing field errors
     * @param request the HTTP request that triggered the error
     * @return a 400 Bad Request response with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed on request to '{}' with {} field error(s)", request.getRequestURI(), ex.getBindingResult().getFieldErrorCount());

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Catches any unhandled exceptions as a fallback handler.
     *
     * @param ex      the unexpected exception
     * @param request the HTTP request that triggered the error
     * @return a 500 Internal Server Error response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

