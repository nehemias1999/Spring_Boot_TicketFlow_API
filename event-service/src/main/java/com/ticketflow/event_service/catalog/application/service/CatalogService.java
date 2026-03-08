package com.ticketflow.event_service.catalog.application.service;

import com.ticketflow.event_service.catalog.application.dto.response.CatalogResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateCatalogRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateCatalogRequest;
import com.ticketflow.event_service.catalog.application.mapper.ICatalogApplicationMapper;
import com.ticketflow.event_service.catalog.domain.exception.CatalogAlreadyExistsException;
import com.ticketflow.event_service.catalog.domain.exception.CatalogNotFoundException;
import com.ticketflow.event_service.catalog.domain.model.Catalog;
import com.ticketflow.event_service.catalog.domain.port.in.ICatalogService;
import com.ticketflow.event_service.catalog.domain.port.out.ICatalogPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the {@link ICatalogService} inbound port.
 * <p>
 * Contains all business logic for catalog CRUD operations including
 * existence validation, soft-delete handling, and delegation to the
 * persistence outbound port.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService implements ICatalogService {

    private final ICatalogPersistencePort catalogPersistencePort;
    private final ICatalogApplicationMapper catalogApplicationMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Validates that no active catalog with the same ID exists before persisting.
     * </p>
     */
    @Override
    public CatalogResponse create(CreateCatalogRequest request) {
        log.info("Creating catalog entry with id: {}", request.id());

        if (catalogPersistencePort.existsByIdAndDeletedFalse(request.id())) {
            log.warn("Catalog creation failed - catalog with id '{}' already exists", request.id());
            throw new CatalogAlreadyExistsException(request.id());
        }

        Catalog catalog = catalogApplicationMapper.toDomain(request);
        Catalog savedCatalog = catalogPersistencePort.save(catalog);

        log.info("Catalog entry created successfully with id: {}", savedCatalog.getId());
        return catalogApplicationMapper.toResponse(savedCatalog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CatalogResponse getById(String id) {
        log.info("Retrieving catalog entry with id: {}", id);

        Catalog catalog = catalogPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Catalog retrieval failed - catalog with id '{}' not found", id);
                    return new CatalogNotFoundException(id);
                });

        log.info("Catalog entry retrieved successfully with id: {}", id);
        return catalogApplicationMapper.toResponse(catalog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CatalogResponse> getAll(Pageable pageable) {
        log.info("Retrieving all catalog entries - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<CatalogResponse> result = catalogPersistencePort.findAllByDeletedFalse(pageable)
                .map(catalogApplicationMapper::toResponse);

        log.info("Retrieved {} catalog entries out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the existing entry, applies updates from the request,
     * and persists the modified domain object.
     * </p>
     */
    @Override
    public CatalogResponse update(String id, UpdateCatalogRequest request) {
        log.info("Updating catalog entry with id: {}", id);

        Catalog existingCatalog = catalogPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Catalog update failed - catalog with id '{}' not found", id);
                    return new CatalogNotFoundException(id);
                });

        catalogApplicationMapper.updateDomainFromRequest(request, existingCatalog);
        Catalog savedCatalog = catalogPersistencePort.update(existingCatalog);

        log.info("Catalog entry updated successfully with id: {}", savedCatalog.getId());
        return catalogApplicationMapper.toResponse(savedCatalog);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs a soft delete by setting the {@code deleted} flag to {@code true}.
     * The record remains in the database but is excluded from active queries.
     * </p>
     */
    @Override
    public void delete(String id) {
        log.info("Soft-deleting catalog entry with id: {}", id);

        Catalog catalog = catalogPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Catalog soft-delete failed - catalog with id '{}' not found", id);
                    return new CatalogNotFoundException(id);
                });

        catalog.setDeleted(true);
        catalogPersistencePort.update(catalog);

        log.info("Catalog entry soft-deleted successfully with id: {}", id);
    }
}

