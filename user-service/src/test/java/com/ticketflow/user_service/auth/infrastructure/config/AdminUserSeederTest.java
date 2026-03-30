package com.ticketflow.user_service.auth.infrastructure.config;

import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserSeeder — unit tests")
class AdminUserSeederTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserSeeder seeder;

    @Test
    @DisplayName("should seed admin user when no admin exists")
    void run_noAdminExists_createsAdmin() throws Exception {
        when(userPersistencePort.existsByEmail("admin@ticketflow.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");

        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@ticketflow.com");
        assertThat(saved.getUsername()).isEqualTo("ADMIN");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getPassword()).isEqualTo("$2a$hashed");
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("should skip seeding when admin already exists")
    void run_adminExists_skipsCreation() throws Exception {
        when(userPersistencePort.existsByEmail("admin@ticketflow.com")).thenReturn(true);

        seeder.run(null);

        verify(userPersistencePort, never()).save(any());
    }
}
