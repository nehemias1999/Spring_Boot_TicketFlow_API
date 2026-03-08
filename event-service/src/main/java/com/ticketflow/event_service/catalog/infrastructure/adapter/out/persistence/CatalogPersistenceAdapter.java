package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import com.ticketflow.event_service.catalog.domain.model.Catalog;
import com.ticketflow.event_service.catalog.domain.port.out.ICatalogPersistencePort;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper.ICatalogPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing the {@link ICatalogPersistencePort} outbound port.
 * <p>
 * This adapter bridges the domain layer with the JPA infrastructure,
 * converting between domain objects and JPA entities using
 * {@link ICatalogPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogPersistenceAdapter implements ICatalogPersistencePort {

    private final ICatalogJpaRepository catalogJpaRepository;
    private final ICatalogPersistenceMapper catalogPersistenceMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog save(Catalog catalog) {
        log.debug("Saving catalog entity with id: {}", catalog.getId());
        CatalogEntity entity = catalogPersistenceMapper.toEntity(catalog);
        CatalogEntity savedEntity = catalogJpaRepository.save(entity);
        log.debug("Catalog entity saved successfully with id: {}", savedEntity.getId());
        return catalogPersistenceMapper.toDomain(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Catalog> findByIdAndDeletedFalse(String id) {
        log.debug("Finding active catalog entity with id: {}", id);
        Optional<Catalog> result = catalogJpaRepository.findByIdAndDeletedFalse(id)
                .map(catalogPersistenceMapper::toDomain);
        log.debug("Catalog entity with id '{}' {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Catalog> findAllByDeletedFalse(Pageable pageable) {
        log.debug("Finding all active catalog entities - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Catalog> result = catalogJpaRepository.findAllByDeletedFalse(pageable)
                .map(catalogPersistenceMapper::toDomain);
        log.debug("Found {} active catalog entities out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByIdAndDeletedFalse(String id) {
        log.debug("Checking existence of active catalog entity with id: {}", id);
        boolean exists = catalogJpaRepository.existsByIdAndDeletedFalse(id);
        log.debug("Active catalog entity with id '{}' exists: {}", id, exists);
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog update(Catalog catalog) {
        log.debug("Updating catalog entity with id: {}", catalog.getId());
        CatalogEntity entity = catalogPersistenceMapper.toEntity(catalog);
        CatalogEntity updatedEntity = catalogJpaRepository.save(entity);
        log.debug("Catalog entity updated successfully with id: {}", updatedEntity.getId());
        return catalogPersistenceMapper.toDomain(updatedEntity);
    }
}

