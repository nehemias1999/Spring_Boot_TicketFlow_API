package com.ticketflow.user_service.auth.infrastructure.adapter.in.web;

import com.ticketflow.user_service.auth.application.dto.request.ChangePasswordRequest;
import com.ticketflow.user_service.auth.application.dto.request.UpdateProfileRequest;
import com.ticketflow.user_service.auth.application.dto.request.UpdateRoleRequest;
import com.ticketflow.user_service.auth.application.dto.response.UserResponse;
import com.ticketflow.user_service.auth.domain.port.in.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user management endpoints.
 * <p>
 * Base path: {@code /api/v1/users}
 * </p>
 * <ul>
 *   <li>GET  /api/v1/users          — list all users (ADMIN only)</li>
 *   <li>GET  /api/v1/users/{id}     — get user by id (ADMIN/MODERATOR any; USER own profile)</li>
 *   <li>PATCH /api/v1/users/{id}/role — change user role (ADMIN any; MODERATOR USER↔SELLER only)</li>
 * </ul>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users and their roles")
public class UserController {

    private final IUserService userServicePort;

    @Operation(summary = "List all users (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            throw new com.ticketflow.user_service.auth.domain.exception.AccessDeniedException(
                    "Only ADMIN can list all users");
        }

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.info("GET /api/v1/users - listing all users, page: {}", page);
        return ResponseEntity.ok(userServicePort.getAllUsers(pageable));
    }

    @Operation(summary = "Get user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String requestingUserId,
            @RequestHeader("X-User-Role") String requestingUserRole) {

        log.info("GET /api/v1/users/{} - requested by userId: {}", id, requestingUserId);
        UserResponse response = userServicePort.getUserById(id, requestingUserId, requestingUserRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user role (ADMIN: any role; MODERATOR: USER↔SELLER only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid role"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request,
            @RequestHeader("X-User-Role") String requestingUserRole) {

        if (!"ADMIN".equalsIgnoreCase(requestingUserRole)
                && !"MODERATOR".equalsIgnoreCase(requestingUserRole)) {
            throw new com.ticketflow.user_service.auth.domain.exception.AccessDeniedException(
                    "Only ADMIN or MODERATOR can change user roles");
        }

        log.info("PATCH /api/v1/users/{}/role - new role: {}, requested by role: {}",
                id, request.role(), requestingUserRole);
        UserResponse response = userServicePort.updateUserRole(id, request.role(), requestingUserRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user profile (own profile or ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Username or email already taken")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader("X-User-Id") String requestingUserId,
            @RequestHeader("X-User-Role") String requestingUserRole) {
        log.info("PUT /api/v1/users/{} - requestedBy: {}", id, requestingUserId);
        UserResponse response = userServicePort.updateProfile(id, request, requestingUserId, requestingUserRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change user password (own account only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or wrong current password"),
            @ApiResponse(responseCode = "403", description = "Cannot change another user's password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("X-User-Id") String requestingUserId) {
        log.info("PUT /api/v1/users/{}/password - requestedBy: {}", id, requestingUserId);
        userServicePort.changePassword(id, request, requestingUserId);
        return ResponseEntity.noContent().build();
    }
}
