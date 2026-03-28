package com.ticketflow.user_service.auth.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
import com.ticketflow.user_service.auth.domain.port.in.IUserService;
import com.ticketflow.user_service.auth.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AuthController} using the Spring MVC test slice.
 *
 * @author TicketFlow Team
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=testSecretKeyForJwtTokenGenerationThatIsLongEnough123456",
        "jwt.expiration-ms=86400000"
})
@DisplayName("AuthController — unit tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUserService userService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AuthResponse buildAuthResponse() {
        return new AuthResponse("jwt-token", "user-uuid", "test@example.com", "USER");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/register
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 Created with AuthResponse body on successful registration")
        void register_success_returns201() throws Exception {
            RegisterRequest request = new RegisterRequest("test@example.com", "password123");
            AuthResponse response = buildAuthResponse();

            when(userService.register(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.userId").value("user-uuid"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when email is blank")
        void register_blankEmail_returns400() throws Exception {
            String invalidBody = """
                    {
                      "email": "",
                      "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 Bad Request when password is too short")
        void register_shortPassword_returns400() throws Exception {
            String invalidBody = """
                    {
                      "email": "test@example.com",
                      "password": "abc"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 Bad Request when email format is invalid")
        void register_invalidEmailFormat_returns400() throws Exception {
            String invalidBody = """
                    {
                      "email": "not-an-email",
                      "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 409 Conflict when email is already registered")
        void register_emailExists_returns409() throws Exception {
            RegisterRequest request = new RegisterRequest("existing@example.com", "password123");

            when(userService.register(any())).thenThrow(new UserAlreadyExistsException("existing@example.com"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("User with email 'existing@example.com' already exists"));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 OK with AuthResponse body on successful login")
        void login_success_returns200() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            AuthResponse response = buildAuthResponse();

            when(userService.login(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.userId").value("user-uuid"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when login request body is invalid")
        void login_invalidBody_returns400() throws Exception {
            String invalidBody = """
                    {
                      "email": "not-valid",
                      "password": ""
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when credentials are invalid")
        void login_invalidCredentials_returns401() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

            when(userService.login(any())).thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }
}
