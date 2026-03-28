package com.ticketflow.user_service.auth.domain.port.out;

import com.ticketflow.user_service.auth.domain.model.User;

import java.util.Optional;

/**
 * Outbound port defining the persistence operations available for user management.
 * <p>
 * Implemented by the persistence adapter in the infrastructure layer and called
 * by the application service.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface IUserPersistencePort {

    /**
     * Persists a new or updated user record.
     *
     * @param user the user domain object to save
     * @return the saved user domain object with any infrastructure-populated fields
     */
    User save(User user);

    /**
     * Finds an active (non-deleted) user by their email address.
     *
     * @param email the user's email address
     * @return an {@link Optional} containing the user if found and not deleted
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * Checks whether any user (active or deleted) is registered with the given email.
     *
     * @param email the email address to check
     * @return {@code true} if a user with the given email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
