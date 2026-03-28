package com.ticketflow.user_service.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Global CORS configuration for the user-service.
 * <p>
 * Permits all origins, methods, and headers for development and
 * internal microservice communication. In production, restrict
 * {@code allowedOrigins} to the known frontend and gateway origins.
 * </p>
 *
 * @author TicketFlow Team
 */
@Configuration
public class CorsConfig {

    /**
     * Registers a {@link CorsFilter} that applies permissive CORS settings
     * to all request paths.
     *
     * @return a fully configured {@link CorsFilter} bean
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
