package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the {@code catalogs} table in the database.
 * <p>
 * This class is an infrastructure concern and should not leak into
 * the domain or application layers. Conversion to/from the domain
 * model is handled by {@link com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Entity
@Table(name = "catalogs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogEntity {

    /**
     * Unique business identifier provided by the client (e.g., "EVT-001").
     */
    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    /**
     * Title or name of the event.
     */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /**
     * Short description of the event.
     */
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /**
     * Date and time of the event as a string.
     */
    @Column(name = "date", nullable = false)
    private String date;

    /**
     * Venue or location of the event.
     */
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    /**
     * Base reference price for the event.
     */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    /**
     * Soft-delete flag. {@code true} means the record is logically deleted.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp when the record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated. {@code null} if never updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
