package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing the {@link IUserPersistencePort} outbound port.
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements IUserPersistencePort {

    private final IUserJpaRepository userJpaRepository;
    private final IUserPersistenceMapper userPersistenceMapper;

    @Override
    public User save(User user) {
        log.debug("Saving user entity with id: {}", user.getId());
        UserEntity entity = userPersistenceMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return userPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmailAndDeletedFalse(String email) {
        log.debug("Finding active user by email: {}", email);
        return userJpaRepository.findByEmailAndDeletedFalse(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameAndDeletedFalse(String username) {
        log.debug("Finding active user by username: {}", username);
        return userJpaRepository.findByUsernameAndDeletedFalse(username)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByIdAndDeletedFalse(String id) {
        log.debug("Finding active user by id: {}", id);
        return userJpaRepository.findByIdAndDeletedFalse(id)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Page<User> findAllByDeletedFalse(Pageable pageable) {
        log.debug("Fetching all active users, page: {}", pageable.getPageNumber());
        return userJpaRepository.findAllByDeletedFalse(pageable)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public User update(User user) {
        log.debug("Updating user entity with id: {}", user.getId());
        UserEntity entity = userPersistenceMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return userPersistenceMapper.toDomain(savedEntity);
    }
}
