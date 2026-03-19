package com.ticketflow.event_service.shared.infrastructure.exception;

import com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — unit tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/events/EVT-001");
    }

    // -------------------------------------------------------------------------
    // handleCatalogNotFoundException — EventNotFoundException → 404
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleCatalogNotFoundException() — EventNotFoundException")
    class HandleEventNotFoundException {

        @Test
        @DisplayName("should return 404 with error details when EventNotFoundException is thrown")
        void handleEventNotFoundException_returns404() {
            // given
            EventNotFoundException ex = new EventNotFoundException("EVT-001");

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleCatalogNotFoundException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).contains("EVT-001");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/events/EVT-001");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // handleValidationException — MethodArgumentNotValidException → 400
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleValidationException() — MethodArgumentNotValidException")
    class HandleValidationException {

        @Test
        @DisplayName("should return 400 with field error message for single validation failure")
        void handleValidationException_singleError_returns400() {
            // given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("createEventRequest", "title", "Title is required");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
            when(bindingResult.getFieldErrorCount()).thenReturn(1);

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleValidationException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().message())
                    .contains("title")
                    .contains("Title is required");
        }

        @Test
        @DisplayName("should concatenate multiple field errors into a single message")
        void handleValidationException_multipleErrors_concatenatesAll() {
            // given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError titleError = new FieldError("createEventRequest", "title", "Title is required");
            FieldError descError = new FieldError("createEventRequest", "description", "Description is required");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(titleError, descError));
            when(bindingResult.getFieldErrorCount()).thenReturn(2);

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleValidationException(ex, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message())
                    .contains("title")
                    .contains("description");
        }
    }

    // -------------------------------------------------------------------------
    // handleGenericException — Exception → 500
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleGenericException() — unexpected Exception")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 with generic message for unexpected exceptions")
        void handleGenericException_returns500() {
            // given
            Exception ex = new RuntimeException("Unexpected database failure");

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleGenericException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().message())
                    .isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/events/EVT-001");
        }
    }
}
