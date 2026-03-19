package com.ticketflow.ticket_service.shared.infrastructure.exception;

import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
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
        when(request.getRequestURI()).thenReturn("/api/v1/tickets/TKT-001");
    }

    // -------------------------------------------------------------------------
    // handleTicketNotFoundException — TicketNotFoundException → 404
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleTicketNotFoundException() — TicketNotFoundException")
    class HandleTicketNotFoundException {

        @Test
        @DisplayName("should return 404 with error details when TicketNotFoundException is thrown")
        void handleTicketNotFoundException_returns404() {
            // given
            TicketNotFoundException ex = new TicketNotFoundException("TKT-001");

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleTicketNotFoundException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).contains("TKT-001");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/tickets/TKT-001");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // handleTicketAlreadyCancelledException — TicketAlreadyCancelledException → 409
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleTicketAlreadyCancelledException() — TicketAlreadyCancelledException")
    class HandleTicketAlreadyCancelledException {

        @Test
        @DisplayName("should return 409 with error details when TicketAlreadyCancelledException is thrown")
        void handleTicketAlreadyCancelledException_returns409() {
            // given
            TicketAlreadyCancelledException ex = new TicketAlreadyCancelledException("TKT-001");

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleTicketAlreadyCancelledException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().error()).isEqualTo("Conflict");
            assertThat(response.getBody().message()).contains("TKT-001");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/tickets/TKT-001");
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
            FieldError fieldError = new FieldError("createTicketRequest", "eventId", "must not be blank");

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
                    .contains("eventId")
                    .contains("must not be blank");
        }

        @Test
        @DisplayName("should concatenate multiple field errors into a single message")
        void handleValidationException_multipleErrors_concatenatesAll() {
            // given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError eventIdError = new FieldError("createTicketRequest", "eventId", "must not be blank");
            FieldError userIdError = new FieldError("createTicketRequest", "userId", "must not be blank");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(eventIdError, userIdError));
            when(bindingResult.getFieldErrorCount()).thenReturn(2);

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleValidationException(ex, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message())
                    .contains("eventId")
                    .contains("userId");
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
            assertThat(response.getBody().path()).isEqualTo("/api/v1/tickets/TKT-001");
        }
    }
}
