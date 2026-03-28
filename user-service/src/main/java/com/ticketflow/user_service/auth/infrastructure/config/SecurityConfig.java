package com.ticketflow.user_service.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the user-service.
 * <p>
 * Configures the service as a stateless API:
 * <ul>
 *   <li>CSRF is disabled (not needed for stateless JWT-based APIs)</li>
 *   <li>Session management is set to {@link SessionCreationPolicy#STATELESS}</li>
 *   <li>All requests are permitted — JWT validation is handled by the API Gateway,
 *       which acts as the security perimeter for the internal microservices</li>
 * </ul>
 * </p>
 *
 * @author TicketFlow Team
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     * <p>
     * Disables CSRF protection, sets session management to stateless,
     * and permits all incoming requests. The API Gateway handles JWT
     * validation before forwarding requests to this service.
     * </p>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs while building the filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Provides a {@link BCryptPasswordEncoder} bean for hashing and verifying passwords.
     *
     * @return a BCrypt password encoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
