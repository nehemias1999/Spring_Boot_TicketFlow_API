package com.ticketflow.user_service.auth.application.service;

import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.application.dto.response.UserResponse;
import com.ticketflow.user_service.auth.application.mapper.IUserApplicationMapper;
import com.ticketflow.user_service.auth.domain.exception.AccessDeniedException;
import com.ticketflow.user_service.auth.domain.exception.InvalidCredentialsException;
import com.ticketflow.user_service.auth.domain.exception.InvalidRoleException;
import com.ticketflow.user_service.auth.domain.exception.UserAlreadyExistsException;
import com.ticketflow.user_service.auth.domain.exception.UserNotFoundException;
import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import com.ticketflow.user_service.auth.domain.port.in.IUserService;
import com.ticketflow.user_service.auth.domain.port.out.IUserEventPublisher;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import com.ticketflow.user_service.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing the {@link IUserService} inbound port.
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

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration request for email: {}", request.email());

        if (userPersistencePort.existsByEmail(request.email())) {
            log.warn("Registration failed - email '{}' already registered", request.email());
            throw new UserAlreadyExistsException(request.email());
        }

        if (userPersistencePort.existsByUsername(request.username())) {
            log.warn("Registration failed - username '{}' already taken", request.username());
            throw new UserAlreadyExistsException("username: " + request.username());
        }

        String id = UUID.randomUUID().toString();
        User user = userApplicationMapper.toDomain(request);
        user.setId(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);

        User savedUser = userPersistencePort.save(user);

        String token = jwtUtil.generateToken(
                savedUser.getId(), savedUser.getUsername(),
                savedUser.getEmail(), savedUser.getRole().name());

        userEventPublisher.publishUserRegistered(savedUser.getId(), savedUser.getEmail());

        log.info("User registered successfully with id: {}", savedUser.getId());
        return new AuthResponse(token, savedUser.getId(), savedUser.getUsername(),
                savedUser.getEmail(), savedUser.getRole().name());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login request for identifier: {}", request.identifier());

        User user = resolveUserByIdentifier(request.identifier());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed - invalid password for identifier: {}", request.identifier());
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(
                user.getId(), user.getUsername(),
                user.getEmail(), user.getRole().name());

        log.info("User logged in successfully with id: {}", user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(),
                user.getEmail(), user.getRole().name());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users, page: {}", pageable.getPageNumber());
        return userPersistencePort.findAllByDeletedFalse(pageable)
                .map(userApplicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id, String requestingUserId, String requestingUserRole) {
        log.info("Fetching user by id: {}", id);

        boolean isPrivileged = "ADMIN".equalsIgnoreCase(requestingUserRole)
                || "MODERATOR".equalsIgnoreCase(requestingUserRole);

        if (!isPrivileged && !id.equals(requestingUserId)) {
            throw new AccessDeniedException("You can only view your own profile");
        }

        User user = userPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return userApplicationMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUserRole(String id, String newRole, String requestingUserRole) {
        log.info("Updating role of user '{}' to '{}', requested by role '{}'",
                id, newRole, requestingUserRole);

        UserRole targetRole;
        try {
            targetRole = UserRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException(newRole);
        }

        if ("MODERATOR".equalsIgnoreCase(requestingUserRole)) {
            if (targetRole == UserRole.ADMIN || targetRole == UserRole.MODERATOR) {
                throw new AccessDeniedException(
                        "Moderators can only assign USER or SELLER roles");
            }
        }

        User user = userPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setRole(targetRole);
        User updated = userPersistencePort.update(user);

        log.info("Role of user '{}' updated to '{}'", id, targetRole);
        return userApplicationMapper.toResponse(updated);
    }

    private User resolveUserByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userPersistencePort.findByEmailAndDeletedFalse(identifier)
                    .orElseThrow(() -> {
                        log.warn("Login failed - no active user with email: {}", identifier);
                        return new InvalidCredentialsException();
                    });
        }
        return userPersistencePort.findByUsernameAndDeletedFalse(identifier)
                .orElseThrow(() -> {
                    log.warn("Login failed - no active user with username: {}", identifier);
                    return new InvalidCredentialsException();
                });
    }
}
