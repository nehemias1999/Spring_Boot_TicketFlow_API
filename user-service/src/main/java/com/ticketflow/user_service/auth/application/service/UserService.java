package com.ticketflow.user_service.auth.application.service;

import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.application.mapper.IUserApplicationMapper;
import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import com.ticketflow.user_service.auth.domain.port.in.IUserService;
import com.ticketflow.user_service.auth.domain.port.out.IUserEventPublisher;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import com.ticketflow.user_service.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing the {@link IUserService} inbound port.
 * <p>
 * Contains all business logic for user registration and authentication,
 * including email uniqueness checks, password hashing with BCrypt,
 * and JWT token generation via {@link JwtUtil}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {

    private final IUserPersistencePort userPersistencePort;
    private final IUserApplicationMapper userApplicationMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IUserEventPublisher userEventPublisher;

    /**
     * {@inheritDoc}
     * <p>
     * Verifies that the email is not already in use, generates a UUID as the user ID,
     * hashes the password with BCrypt, assigns the default {@link UserRole#USER} role,
     * persists the user, and returns a signed JWT in the response.
     * </p>
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration request for email: {}", request.email());

        if (userPersistencePort.existsByEmail(request.email())) {
            log.warn("Registration failed - email '{}' is already registered", request.email());
            throw new UserAlreadyExistsException(request.email());
        }

        String id = UUID.randomUUID().toString();
        log.info("Registering new user with generated id: {}", id);

        User user = userApplicationMapper.toDomain(request);
        user.setId(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);

        User savedUser = userPersistencePort.save(user);

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        userEventPublisher.publishUserRegistered(savedUser.getId(), savedUser.getEmail());

        log.info("User registered successfully with id: {}", savedUser.getId());
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Looks up the user by email, verifies the provided password against the stored
     * BCrypt hash, and returns a signed JWT on success. Throws
     * {@link InvalidCredentialsException} if the email is not found or the password
     * does not match, deliberately using a generic message to prevent enumeration attacks.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login request for email: {}", request.email());

        User user = userPersistencePort.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - no active user found with email: {}", request.email());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed - invalid password for email: {}", request.email());
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        log.info("User logged in successfully with id: {}", user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }
}
