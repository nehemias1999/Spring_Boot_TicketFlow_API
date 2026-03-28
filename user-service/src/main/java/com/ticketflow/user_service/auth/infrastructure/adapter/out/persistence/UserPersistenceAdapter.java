package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing the {@link IUserPersistencePort} outbound port.
 * <p>
 * This adapter bridges the domain layer with the JPA infrastructure,
 * converting between domain objects and JPA entities using
 * {@link IUserPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements IUserPersistencePort {

    private final IUserJpaRepository userJpaRepository;
    private final IUserPersistenceMapper userPersistenceMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Converts the domain object to a JPA entity, persists it via the JPA repository,
     * and converts the saved entity back to a domain object.
     * </p>
     */
    @Override
    public User save(User user) {
        log.debug("Saving user entity with id: {}", user.getId());
        UserEntity entity = userPersistenceMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        log.debug("User entity saved successfully with id: {}", savedEntity.getId());
        return userPersistenceMapper.toDomain(savedEntity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Queries the JPA repository for an active entity by email and maps it to a
     * domain object if present. Returns {@link Optional#empty()} if the entity
     * does not exist or has been soft-deleted.
     * </p>
     */
    @Override
    public Optional<User> findByEmailAndDeletedFalse(String email) {
        log.debug("Finding active user entity with email: {}", email);
        Optional<User> result = userJpaRepository.findByEmailAndDeletedFalse(email)
                .map(userPersistenceMapper::toDomain);
        log.debug("User entity with email '{}' {}", email, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates directly to the JPA repository's derived {@code existsByEmail} method.
     * </p>
     */
    @Override
    public boolean existsByEmail(String email) {
        log.debug("Checking existence of user with email: {}", email);
        boolean exists = userJpaRepository.existsByEmail(email);
        log.debug("User with email '{}' {}", email, exists ? "exists" : "does not exist");
        return exists;
    }
}
