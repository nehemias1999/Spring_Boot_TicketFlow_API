package com.ticketflow.user_service.shared.infrastructure.exception;

import com.ticketflow.user_service.auth.domain.exception.AccessDeniedException;
import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.InvalidRoleException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
import com.ticketflow.user_service.auth.domain.exception.UserNotFoundException;
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
 * Handles domain-specific exceptions (already exists, invalid credentials),
 * validation errors, and unexpected server errors. Includes the
 * {@code X-Correlation-Id} header value in every error response for
 * distributed tracing.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    /**
     * Handles {@link UserAlreadyExistsException} when the requested email is already registered.
     *
     * @param ex      the exception thrown
     * @param request the HTTP request that triggered the error
     * @return a 409 Conflict response with error details
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User already exists on request to '{}': {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles {@link InvalidCredentialsException} when login fails due to bad credentials.
     *
     * @param ex      the exception thrown
     * @param request the HTTP request that triggered the error
     * @return a 401 Unauthorized response with error details
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials on request to '{}': {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on request to '{}': {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found on request to '{}': {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRoleException(
            InvalidRoleException ex, HttpServletRequest request) {
        log.warn("Invalid role on request to '{}': {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
        log.warn("Validation failed on request to '{}' with {} field error(s)",
                request.getRequestURI(), ex.getBindingResult().getFieldErrorCount());

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
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
                request.getRequestURI(),
                request.getHeader(CORRELATION_HEADER)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
