package com.ticketflow.ticket_service.booking.domain.port.out;

import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port defining the persistence operations available for ticket management.
 * <p>
 * Implemented by the persistence adapter in the infrastructure layer and called
 * by the application service.
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ITicketPersistencePort {

    /**
     * Persists a new ticket.
     *
     * @param ticket the ticket domain object to save
     * @return the saved ticket domain object
     */
    Ticket save(Ticket ticket);

    /**
     * Finds an active (non-deleted) ticket by its ID.
     *
     * @param id the ticket identifier
     * @return an {@link Optional} containing the ticket if found and not deleted
     */
    Optional<Ticket> findByIdAndDeletedFalse(String id);

    /**
     * Retrieves a paginated and filtered list of active tickets.
     *
     * @param eventId  optional filter by event ID
     * @param userId   optional filter by user ID
     * @param status   optional filter by ticket status string
     * @param pageable pagination and sorting parameters
     * @return a page of ticket domain objects
     */
    Page<Ticket> findAllByFilters(String eventId, String userId, String status, Pageable pageable);

    /**
     * Updates an existing ticket.
     *
     * @param ticket the ticket domain object with updated values
     * @return the updated ticket domain object
     */
    Ticket update(Ticket ticket);

    /**
     * Retrieves a paginated list of active tickets whose eventId is in the given list.
     * Used for SELLER ticket listing.
     *
     * @param eventIds list of event IDs owned by the SELLER
     * @param pageable pagination and sorting parameters
     * @return a page of ticket domain objects
     */
    Page<Ticket> findByEventIdsIn(List<String> eventIds, Pageable pageable);
}
