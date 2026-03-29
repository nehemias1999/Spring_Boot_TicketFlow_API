package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 *
 * @author TicketFlow Team
 */
public interface IUserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    Optional<UserEntity> findByIdAndDeletedFalse(String id);

    Page<UserEntity> findAllByDeletedFalse(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
