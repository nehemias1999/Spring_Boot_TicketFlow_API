package com.ticketflow.user_service.auth.domain.port.in;

import com.ticketflow.user_service.auth.application.dto.request.ChangePasswordRequest;
import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.request.UpdateProfileRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;
import com.ticketflow.user_service.auth.application.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the authentication and user management use cases.
 *
 * @author TicketFlow Team
 */
public interface IUserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(String id, String requestingUserId, String requestingUserRole);

    UserResponse updateUserRole(String id, String newRole, String requestingUserRole);

    UserResponse updateProfile(String id, UpdateProfileRequest request, String requestingUserId, String requestingUserRole);

    void changePassword(String id, ChangePasswordRequest request, String requestingUserId);
}
