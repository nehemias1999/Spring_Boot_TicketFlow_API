package com.ticketflow.event_service.catalog.domain.exception;

/**
 * Exception thrown when attempting to create a catalog entry with an ID
 * that already exists in the system.
 *
 * @author TicketFlow Team
 */
public class CatalogAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new {@code CatalogAlreadyExistsException} with a descriptive message.
     *
     * @param id the duplicate catalog ID
     */
    public CatalogAlreadyExistsException(String id) {
        super(String.format("Catalog with id '%s' already exists", id));
    }
}

