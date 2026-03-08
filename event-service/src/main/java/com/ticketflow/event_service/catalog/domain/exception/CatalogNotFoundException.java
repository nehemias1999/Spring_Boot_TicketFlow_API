package com.ticketflow.event_service.catalog.domain.exception;

/**
 * Exception thrown when a catalog entry with the specified ID is not found
 * or has been soft-deleted.
 *
 * @author TicketFlow Team
 */
public class CatalogNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code CatalogNotFoundException} with a descriptive message.
     *
     * @param id the catalog ID that was not found
     */
    public CatalogNotFoundException(String id) {
        super(String.format("Catalog with id '%s' not found", id));
    }
}

