package com.ticketflow.user_service.auth.infrastructure.config;

import com.ticketflow.user_service.auth.domain.model.User;
import com.ticketflow.user_service.auth.domain.model.UserRole;
import com.ticketflow.user_service.auth.domain.port.out.IUserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds the default ADMIN user on application startup if it does not already exist.
 * <p>
 * This runner is idempotent: it checks for the admin email before creating the user,
 * so it is safe to execute on every startup.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "ADMIN";
    private static final String ADMIN_EMAIL    = "admin@ticketflow.com";
    private static final String ADMIN_PASSWORD = "ADMIN123";

    private final IUserPersistencePort userPersistencePort;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userPersistencePort.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin user already exists, skipping seed");
            return;
        }

        User admin = User.builder()
                .id(UUID.randomUUID().toString())
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(UserRole.ADMIN)
                .deleted(false)
                .build();

        userPersistencePort.save(admin);
        log.info("Default admin user seeded successfully (email: {})", ADMIN_EMAIL);
    }
}
