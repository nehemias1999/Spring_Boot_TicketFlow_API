package com.ticketflow.event_service.catalog.domain.port.out;

import com.ticketflow.event_service.catalog.domain.model.Event;
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
public interface IEventPersistencePort {

    /**
     * Persists a new event domain object.
     *
     * @param event the Event to save
     * @return the persisted event with any generated metadata
     */
    Event save(Event event);

    /**
     * Finds an active (non-deleted) event by its unique ID.
     *
     * @param id the unique business identifier
     * @return an {@link Optional} containing the event if found, or empty otherwise
     */
    Optional<Event> findByIdAndDeletedFalse(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) event entries.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of event domain objects
     */
    Page<Event> findAllByDeletedFalse(Pageable pageable);

    /**
     * Checks whether an active (non-deleted) event with the given ID exists.
     *
     * @param id the unique business identifier
     * @return {@code true} if an active event exists, {@code false} otherwise
     */
    boolean existsByIdAndDeletedFalse(String id);

    /**
     * Updates an existing event domain object in the persistence store.
     *
     * @param event the event with updated data
     * @return the updated event
     */
    Event update(Event event);
}

