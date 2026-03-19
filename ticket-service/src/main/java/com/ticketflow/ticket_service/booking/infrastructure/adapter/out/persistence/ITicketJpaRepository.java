package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link TicketEntity}.
 * <p>
 * Provides derived query methods that filter out soft-deleted records,
 * ensuring only active ticket entries are returned by default.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ITicketJpaRepository extends JpaRepository<TicketEntity, String>, JpaSpecificationExecutor<TicketEntity> {

    /**
     * Finds an active ticket entity by its ID.
     *
     * @param id the unique business identifier
     * @return an {@link Optional} containing the entity if found and not deleted
     */
    Optional<TicketEntity> findByIdAndDeletedFalse(String id);

}
