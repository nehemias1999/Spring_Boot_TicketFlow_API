package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence;

import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the {@code tickets} table in the database.
 * <p>
 * This class is an infrastructure concern and should not leak into
 * the domain or application layers. Conversion to/from the domain
 * model is handled by {@link mapper.ITicketPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEntity {

    /**
     * Unique business identifier provided by the client (e.g., "TKT-001").
     */
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * Reference to the event this ticket belongs to.
     */
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    /**
     * Reference to the user who owns this ticket.
     */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /**
     * Date and time when the ticket was purchased.
     */
    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;

    /**
     * Current lifecycle status of the ticket, stored as a VARCHAR.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    /**
     * Price paid at the time of purchase. Nullable for tickets created before this feature.
     */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Soft-delete flag. {@code true} means the record is logically deleted.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp when the record was created. Populated automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated. Populated automatically by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
