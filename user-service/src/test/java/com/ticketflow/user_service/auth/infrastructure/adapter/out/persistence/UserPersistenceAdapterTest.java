package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserPersistenceAdapter}.
 *
 * @author TicketFlow Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserPersistenceAdapter — unit tests")
class UserPersistenceAdapterTest {

    @Mock
    private IUserJpaRepository userJpaRepository;

    @Mock
    private IUserPersistenceMapper userPersistenceMapper;

    @InjectMocks
    private UserPersistenceAdapter userPersistenceAdapter;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static User buildDomain(String id) {
        return User.builder()
                .id(id)
                .email("test@example.com")
                .password("$2a$hashed")
                .role(UserRole.USER)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static UserEntity buildEntity(String id) {
        return UserEntity.builder()
                .id(id)
                .email("test@example.com")
                .password("$2a$hashed")
                .role(UserRole.USER)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("should convert to entity, persist, and return domain object")
        void save_success() {
            // given
            User domain = buildDomain("user-001");
            UserEntity entity = buildEntity("user-001");

            when(userPersistenceMapper.toEntity(domain)).thenReturn(entity);
            when(userJpaRepository.save(entity)).thenReturn(entity);
            when(userPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            User result = userPersistenceAdapter.save(domain);

            // then
            assertThat(result).isEqualTo(domain);
            verify(userJpaRepository).save(entity);
        }
    }

    // -------------------------------------------------------------------------
    // findByEmailAndDeletedFalse()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findByEmailAndDeletedFalse()")
    class FindByEmailAndDeletedFalse {

        @Test
        @DisplayName("should return Optional with domain object when entity is found")
        void findByEmailAndDeletedFalse_found() {
            // given
            User domain = buildDomain("user-001");
            UserEntity entity = buildEntity("user-001");

            when(userJpaRepository.findByEmailAndDeletedFalse("test@example.com"))
                    .thenReturn(Optional.of(entity));
            when(userPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Optional<User> result = userPersistenceAdapter.findByEmailAndDeletedFalse("test@example.com");

            // then
            assertThat(result).isPresent().contains(domain);
        }

        @Test
        @DisplayName("should return empty Optional when entity is not found")
        void findByEmailAndDeletedFalse_notFound() {
            // given
            when(userJpaRepository.findByEmailAndDeletedFalse("unknown@example.com"))
                    .thenReturn(Optional.empty());

            // when
            Optional<User> result = userPersistenceAdapter.findByEmailAndDeletedFalse("unknown@example.com");

            // then
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // existsByEmail()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmail {

        @Test
        @DisplayName("should return true when email exists in repository")
        void existsByEmail_exists_returnsTrue() {
            // given
            when(userJpaRepository.existsByEmail("test@example.com")).thenReturn(true);

            // when
            boolean result = userPersistenceAdapter.existsByEmail("test@example.com");

            // then
            assertThat(result).isTrue();
            verify(userJpaRepository).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("should return false when email does not exist in repository")
        void existsByEmail_notExists_returnsFalse() {
            // given
            when(userJpaRepository.existsByEmail("new@example.com")).thenReturn(false);

            // when
            boolean result = userPersistenceAdapter.existsByEmail("new@example.com");

            // then
            assertThat(result).isFalse();
        }
    }
}
