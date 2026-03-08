package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CatalogEntity}.
 * <p>
 * Provides derived query methods that filter out soft-deleted records,
 * ensuring only active catalog entries are returned by default.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ICatalogJpaRepository extends JpaRepository<CatalogEntity, String> {

    /**
     * Finds an active catalog entity by its ID.
     *
     * @param id the unique business identifier
     * @return an {@link Optional} containing the entity if found and not deleted
     */
    Optional<CatalogEntity> findByIdAndDeletedFalse(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) catalog entities.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of active catalog entities
     */
    Page<CatalogEntity> findAllByDeletedFalse(Pageable pageable);

    /**
     * Checks whether an active catalog entity with the given ID exists.
     *
     * @param id the unique business identifier
     * @return {@code true} if an active entity exists, {@code false} otherwise
     */
    boolean existsByIdAndDeletedFalse(String id);
}

