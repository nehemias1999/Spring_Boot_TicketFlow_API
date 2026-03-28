package com.ticketflow.user_service.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing so that {@code @CreatedDate} and {@code @LastModifiedDate}
 * fields on entities are populated automatically by Spring Data.
 *
 * @author TicketFlow Team
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
