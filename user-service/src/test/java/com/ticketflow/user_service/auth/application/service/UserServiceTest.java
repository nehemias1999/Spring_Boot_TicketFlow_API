package com.ticketflow.user_service.auth.application.service;

import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.application.mapper.IUserApplicationMapper;
import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import com.ticketflow.user_service.shared.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 * <p>
 * All dependencies are mocked with Mockito so that only the service
 * business logic is tested in isolation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — unit tests")
class UserServiceTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private IUserApplicationMapper userApplicationMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static User buildUser(String id, String email, String hashedPassword) {
        return User.builder()
                .id(id)
                .email(email)
                .password(hashedPassword)
                .role(UserRole.USER)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static RegisterRequest buildRegisterRequest() {
        return new RegisterRequest("test@example.com", "password123");
    }

    private static LoginRequest buildLoginRequest() {
        return new LoginRequest("test@example.com", "password123");
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should generate UUID id, hash password, save, and return AuthResponse on success")
        void register_success() {
            // given
            RegisterRequest request = buildRegisterRequest();
            User partialUser = User.builder().email("test@example.com").build();
            User savedUser = buildUser("generated-uuid", "test@example.com", "$2a$hashed");

            when(userPersistencePort.existsByEmail("test@example.com")).thenReturn(false);
            when(userApplicationMapper.toDomain(request)).thenReturn(partialUser);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userPersistencePort.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

            // when
            AuthResponse result = userService.register(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("jwt-token");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.role()).isEqualTo("USER");
            verify(userPersistencePort).save(any(User.class));
            verify(passwordEncoder).encode("password123");
            verify(jwtUtil).generateToken(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when email is already registered")
        void register_emailExists_throwsException() {
            // given
            RegisterRequest request = buildRegisterRequest();
            when(userPersistencePort.existsByEmail("test@example.com")).thenReturn(true);

            // when / then
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("test@example.com");

            verify(userPersistencePort, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should return AuthResponse with JWT token when credentials are valid")
        void login_success() {
            // given
            LoginRequest request = buildLoginRequest();
            User existingUser = buildUser("user-uuid", "test@example.com", "$2a$hashed");

            when(userPersistencePort.findByEmailAndDeletedFalse("test@example.com"))
                    .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

            // when
            AuthResponse result = userService.login(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("jwt-token");
            assertThat(result.userId()).isEqualTo("user-uuid");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.role()).isEqualTo("USER");
            verify(jwtUtil).generateToken("user-uuid", "test@example.com", "USER");
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException when email is not found")
        void login_emailNotFound_throwsException() {
            // given
            LoginRequest request = buildLoginRequest();
            when(userPersistencePort.findByEmailAndDeletedFalse("test@example.com"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtUtil, never()).generateToken(any(), any(), any());
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException when password does not match")
        void login_wrongPassword_throwsException() {
            // given
            LoginRequest request = buildLoginRequest();
            User existingUser = buildUser("user-uuid", "test@example.com", "$2a$hashed");

            when(userPersistencePort.findByEmailAndDeletedFalse("test@example.com"))
                    .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(false);

            // when / then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtUtil, never()).generateToken(any(), any(), any());
        }
    }
}
