package com.ticketflow.event_service.catalog.domain.port.in;

import com.ticketflow.event_service.catalog.application.dto.response.CatalogResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateCatalogRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateCatalogRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the use cases available for catalog management.
 * <p>
 * This interface is implemented by the application service layer and
 * consumed by the infrastructure adapters (e.g., REST controller).
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ICatalogService {

    /**
     * Creates a new catalog entry in the system.
     *
     * @param request the data required to create a catalog entry
     * @return the created catalog as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.CatalogAlreadyExistsException
     *         if a catalog with the same ID already exists
     */
    CatalogResponse create(CreateCatalogRequest request);

    /**
     * Retrieves a single catalog entry by its unique ID.
     *
     * @param id the unique business identifier of the catalog
     * @return the catalog data as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.CatalogNotFoundException
     *         if no active catalog with the given ID exists
     */
    CatalogResponse getById(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) catalog entries.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of catalog response DTOs
     */
    Page<CatalogResponse> getAll(Pageable pageable);

    /**
     * Updates an existing catalog entry with the provided data.
     *
     * @param id      the unique business identifier of the catalog to update
     * @param request the data to apply as updates
     * @return the updated catalog as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.CatalogNotFoundException
     *         if no active catalog with the given ID exists
     */
    CatalogResponse update(String id, UpdateCatalogRequest request);

    /**
     * Performs a soft delete on the catalog entry with the given ID.
     * <p>
     * The record is not physically removed; instead, it is marked as deleted.
     * </p>
     *
     * @param id the unique business identifier of the catalog to delete
     * @throws com.ticketflow.event_service.catalog.domain.exception.CatalogNotFoundException
     *         if no active catalog with the given ID exists
     */
    void delete(String id);
}

