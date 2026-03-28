package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 * <p>
 * Provides derived query methods that filter out soft-deleted records,
 * ensuring only active user entries are returned by default.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface IUserJpaRepository extends JpaRepository<UserEntity, String> {

    /**
     * Finds an active (non-deleted) user entity by email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the entity if found and not deleted
     */
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    /**
     * Checks whether any user record (active or deleted) exists with the given email.
     *
     * @param email the email address to check
     * @return {@code true} if a user with the email exists
     */
    boolean existsByEmail(String email);
}
