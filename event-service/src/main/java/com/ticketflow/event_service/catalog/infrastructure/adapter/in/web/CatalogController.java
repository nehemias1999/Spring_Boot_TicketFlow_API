package com.ticketflow.event_service.catalog.infrastructure.adapter.in.web;

import com.ticketflow.event_service.catalog.application.dto.response.CatalogResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateCatalogRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateCatalogRequest;
import com.ticketflow.event_service.catalog.domain.port.in.ICatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD endpoints for catalog management.
 * <p>
 * This is an inbound adapter in the hexagonal architecture. It receives
 * HTTP requests, delegates to the {@link ICatalogService}, and returns
 * appropriate HTTP responses.
 * </p>
 * <p>
 * Base path: {@code /api/v1/catalogs}
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final ICatalogService catalogServicePort;

    /**
     * Creates a new catalog entry.
     *
     * @param request the validated creation request body
     * @return the created catalog with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<CatalogResponse> create(@Valid @RequestBody CreateCatalogRequest request) {
        log.info("POST /api/v1/catalogs - Request received to create catalog with id: {}", request.id());
        CatalogResponse response = catalogServicePort.create(request);
        log.info("POST /api/v1/catalogs - Catalog created successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single catalog entry by its unique ID.
     *
     * @param id the unique business identifier (path variable)
     * @return the catalog data with HTTP 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<CatalogResponse> getById(@PathVariable String id) {
        log.info("GET /api/v1/catalogs/{} - Request received to retrieve catalog", id);
        CatalogResponse response = catalogServicePort.getById(id);
        log.info("GET /api/v1/catalogs/{} - Catalog retrieved successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of all active catalog entries.
     *
     * @param page the page number (zero-based), defaults to 0
     * @param size the number of items per page, defaults to 10
     * @return a page of catalog entries with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<Page<CatalogResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/catalogs - Request received to retrieve all catalogs - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<CatalogResponse> response = catalogServicePort.getAll(pageable);
        log.info("GET /api/v1/catalogs - Retrieved {} catalogs (page {} of {})", response.getNumberOfElements(), response.getNumber(), response.getTotalPages());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing catalog entry with the provided data.
     *
     * @param id      the unique business identifier (path variable)
     * @param request the validated update request body
     * @return the updated catalog with HTTP 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<CatalogResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCatalogRequest request) {
        log.info("PUT /api/v1/catalogs/{} - Request received to update catalog", id);
        CatalogResponse response = catalogServicePort.update(id, request);
        log.info("PUT /api/v1/catalogs/{} - Catalog updated successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a catalog entry by its unique ID.
     * <p>
     * The record is not physically removed from the database; it is
     * marked as deleted and excluded from future active queries.
     * </p>
     *
     * @param id the unique business identifier (path variable)
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/v1/catalogs/{} - Request received to soft-delete catalog", id);
        catalogServicePort.delete(id);
        log.info("DELETE /api/v1/catalogs/{} - Catalog soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}

