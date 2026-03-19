package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link EventEntity}.
 * <p>
 * Provides derived query methods that filter out soft-deleted records,
 * ensuring only active event entries are returned by default.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface IEventJpaRepository extends JpaRepository<EventEntity, String>, JpaSpecificationExecutor<EventEntity> {

    /**
     * Finds an active event entity by its ID.
     *
     * @param id the unique business identifier
     * @return an {@link Optional} containing the entity if found and not deleted
     */
    Optional<EventEntity> findByIdAndDeletedFalse(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) event entities.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of active event entities
     */
    Page<EventEntity> findAllByDeletedFalse(Pageable pageable);

}

