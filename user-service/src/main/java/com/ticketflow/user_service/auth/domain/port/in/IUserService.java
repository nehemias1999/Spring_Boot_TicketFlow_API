package com.ticketflow.user_service.auth.domain.port.in;

import com.ticketflow.user_service.auth.application.dto.request.LoginRequest;
import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.AuthResponse;

/**
 * Inbound port defining the authentication use cases available in the user-service.
 * <p>
 * Implemented by the application service and called by inbound adapters
 * (e.g., REST controllers).
 * </p>
 *
 * @author TicketFlow Team
 */
public interface IUserService {

    /**
     * Registers a new user account and issues a JWT token.
     *
     * @param request the registration request containing the user's email and password
     * @return an {@link AuthResponse} containing the issued JWT token and user details
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates an existing user and issues a JWT token.
     *
     * @param request the login request containing the user's email and password
     * @return an {@link AuthResponse} containing the issued JWT token and user details
     */
    AuthResponse login(LoginRequest request);
}
