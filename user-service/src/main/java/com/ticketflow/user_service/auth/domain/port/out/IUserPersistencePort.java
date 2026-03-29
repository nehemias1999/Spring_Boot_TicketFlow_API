package com.ticketflow.user_service.auth.domain.port.out;

import com.ticketflow.user_service.auth.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Outbound port defining the persistence operations available for user management.
 *
 * @author TicketFlow Team
 */
public interface IUserPersistencePort {

    User save(User user);

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(String id);

    Page<User> findAllByDeletedFalse(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    User update(User user);
}
