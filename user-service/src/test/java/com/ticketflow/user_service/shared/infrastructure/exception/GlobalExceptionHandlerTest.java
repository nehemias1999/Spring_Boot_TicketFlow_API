package com.ticketflow.user_service.shared.infrastructure.exception;

import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
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
 *
 * @author TicketFlow Team
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
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
    }

    // -------------------------------------------------------------------------
    // handleUserAlreadyExistsException — UserAlreadyExistsException → 409
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleUserAlreadyExistsException() — UserAlreadyExistsException")
    class HandleUserAlreadyExistsException {

        @Test
        @DisplayName("should return 409 with error details when UserAlreadyExistsException is thrown")
        void handleUserAlreadyExistsException_returns409() {
            // given
            UserAlreadyExistsException ex = new UserAlreadyExistsException("test@example.com");

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleUserAlreadyExistsException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().error()).isEqualTo("Conflict");
            assertThat(response.getBody().message()).contains("test@example.com");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/register");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // handleInvalidCredentialsException — InvalidCredentialsException → 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleInvalidCredentialsException() — InvalidCredentialsException")
    class HandleInvalidCredentialsException {

        @Test
        @DisplayName("should return 401 with error details when InvalidCredentialsException is thrown")
        void handleInvalidCredentialsException_returns401() {
            // given
            InvalidCredentialsException ex = new InvalidCredentialsException();

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleInvalidCredentialsException(ex, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().error()).isEqualTo("Unauthorized");
            assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/register");
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
            FieldError fieldError = new FieldError("registerRequest", "email", "must be a valid email address");

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
                    .contains("email")
                    .contains("must be a valid email address");
        }

        @Test
        @DisplayName("should concatenate multiple field errors into a single message")
        void handleValidationException_multipleErrors_concatenatesAll() {
            // given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError emailError = new FieldError("registerRequest", "email", "must not be blank");
            FieldError passwordError = new FieldError("registerRequest", "password", "must not be blank");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, passwordError));
            when(bindingResult.getFieldErrorCount()).thenReturn(2);

            // when
            ResponseEntity<ApiErrorResponse> response =
                    handler.handleValidationException(ex, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message())
                    .contains("email")
                    .contains("password");
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
            assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/register");
        }
    }
}
