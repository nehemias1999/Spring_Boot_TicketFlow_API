package com.ticketflow.event_service.catalog.domain.port.out;

import com.ticketflow.event_service.catalog.domain.model.Catalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Outbound port defining the persistence operations required by the domain.
 * <p>
 * This interface is implemented by the infrastructure persistence adapter
 * and consumed by the application service layer.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ICatalogPersistencePort {

    /**
     * Persists a new catalog domain object.
     *
     * @param catalog the catalog to save
     * @return the persisted catalog with any generated metadata
     */
    Catalog save(Catalog catalog);

    /**
     * Finds an active (non-deleted) catalog by its unique ID.
     *
     * @param id the unique business identifier
     * @return an {@link Optional} containing the catalog if found, or empty otherwise
     */
    Optional<Catalog> findByIdAndDeletedFalse(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) catalog entries.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of catalog domain objects
     */
    Page<Catalog> findAllByDeletedFalse(Pageable pageable);

    /**
     * Checks whether an active (non-deleted) catalog with the given ID exists.
     *
     * @param id the unique business identifier
     * @return {@code true} if an active catalog exists, {@code false} otherwise
     */
    boolean existsByIdAndDeletedFalse(String id);

    /**
     * Updates an existing catalog domain object in the persistence store.
     *
     * @param catalog the catalog with updated data
     * @return the updated catalog
     */
    Catalog update(Catalog catalog);
}

