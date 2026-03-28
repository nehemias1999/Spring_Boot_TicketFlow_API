package com.ticketflow.user_service.auth.infrastructure.adapter.in.web;

import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.domain.port.in.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints for user registration and login.
 * <p>
 * This is an inbound adapter in the hexagonal architecture. It receives
 * HTTP requests, delegates to the {@link IUserService}, and returns
 * appropriate HTTP responses containing JWT tokens.
 * </p>
 * <p>
 * Base path: {@code /api/v1/auth}
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class AuthController {

    private final IUserService userServicePort;

    /**
     * Registers a new user account.
     *
     * @param request the registration request containing email and password
     * @return 201 Created with an {@link AuthResponse} containing the JWT token
     */
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register - Request received to register user with email: {}", request.email());
        AuthResponse response = userServicePort.register(request);
        log.info("POST /api/v1/auth/register - User registered successfully with id: {}", response.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates an existing user and issues a JWT token.
     *
     * @param request the login request containing email and password
     * @return 200 OK with an {@link AuthResponse} containing the JWT token
     */
    @Operation(summary = "Log in with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login - Request received to login user with email: {}", request.email());
        AuthResponse response = userServicePort.login(request);
        log.info("POST /api/v1/auth/login - User logged in successfully with id: {}", response.userId());
        return ResponseEntity.ok(response);
    }
}
