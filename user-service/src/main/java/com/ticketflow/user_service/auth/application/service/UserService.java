package com.ticketflow.user_service.auth.application.service;

import com.ticketflow.user_service.auth.application.dto.request.ChangePasswordRequest;
import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.request.UpdateProfileRequest;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        // Publish after commit so a messaging failure never rolls back the registration
        final String newUserId    = savedUser.getId();
        final String newUserEmail = savedUser.getEmail();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    userEventPublisher.publishUserRegistered(newUserId, newUserEmail);
                } catch (Exception e) {
                    log.warn("Failed to publish UserRegisteredEvent for user '{}': {}", newUserId, e.getMessage());
                }
            }
        });

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

    @Override
    public UserResponse updateProfile(String id, UpdateProfileRequest request,
                                      String requestingUserId, String requestingUserRole) {
        log.info("Updating profile of user '{}'", id);

        boolean isPrivileged = "ADMIN".equalsIgnoreCase(requestingUserRole)
                || "MODERATOR".equalsIgnoreCase(requestingUserRole);

        if (!isPrivileged && !id.equals(requestingUserId)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        User user = userPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check new username/email not taken by another user
        if (!user.getUsername().equals(request.username())
                && userPersistencePort.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("username: " + request.username());
        }
        if (!user.getEmail().equals(request.email())
                && userPersistencePort.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        User updated = userPersistencePort.update(user);

        log.info("Profile of user '{}' updated successfully", id);
        return userApplicationMapper.toResponse(updated);
    }

    @Override
    public void changePassword(String id, ChangePasswordRequest request, String requestingUserId) {
        log.info("Changing password of user '{}'", id);

        if (!id.equals(requestingUserId)) {
            throw new AccessDeniedException("You can only change your own password");
        }

        User user = userPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Password change failed - current password incorrect for user '{}'", id);
            throw new InvalidCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userPersistencePort.update(user);
        log.info("Password of user '{}' changed successfully", id);
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
