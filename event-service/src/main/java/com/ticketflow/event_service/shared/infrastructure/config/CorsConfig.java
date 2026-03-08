package com.ticketflow.event_service.shared.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Global CORS (Cross-Origin Resource Sharing) configuration for the microservice.
 * <p>
 * This configuration allows the API Gateway (and other authorized origins)
 * to make cross-origin HTTP requests to this service. Without this,
 * browsers would block requests from the Gateway's domain to this service's domain.
 * </p>
 * <p>
 * In a microservice architecture, the API Gateway acts as the single entry point
 * and forwards requests to downstream services. CORS must be configured to permit
 * these forwarded requests.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * Creates and registers a {@link CorsFilter} bean with permissive CORS settings.
     * <p>
     * Configuration details:
     * <ul>
     *     <li><b>Credentials:</b> Allowed (cookies, authorization headers)</li>
     *     <li><b>Origins:</b> All origins permitted via pattern matching</li>
     *     <li><b>Headers:</b> All headers permitted</li>
     *     <li><b>Methods:</b> All HTTP methods permitted (GET, POST, PUT, DELETE, etc.)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Note:</strong> In a production environment, you should restrict
     * {@code allowedOriginPatterns} to specific trusted domains (e.g., the API Gateway URL)
     * instead of using a wildcard.
     * </p>
     *
     * @return the configured {@link CorsFilter} bean
     */
    @Bean
    public CorsFilter corsFilter() {
        log.info("Initializing CORS filter with permissive configuration for microservice communication");

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS filter initialized successfully — all origins, headers, and methods are allowed");
        return new CorsFilter(source);
    }
}

