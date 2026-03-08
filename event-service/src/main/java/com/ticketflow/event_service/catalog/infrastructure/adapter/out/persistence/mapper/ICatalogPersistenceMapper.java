package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper;

import com.ticketflow.event_service.catalog.domain.model.Catalog;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.CatalogEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between the JPA {@link CatalogEntity}
 * and the domain {@link Catalog} model.
 * <p>
 * This mapper isolates the infrastructure persistence layer from the domain,
 * ensuring that JPA annotations and entity concerns do not leak into
 * the core business logic. The implementation is generated at compile time
 * by the MapStruct annotation processor.
 * </p>
 *
 * @author TicketFlow Team
 */
@Mapper(componentModel = "spring")
public interface ICatalogPersistenceMapper {

    /**
     * Converts a {@link Catalog} domain object to a {@link CatalogEntity} JPA entity.
     *
     * @param catalog the domain object to convert
     * @return the corresponding JPA entity
     */
    CatalogEntity toEntity(Catalog catalog);

    /**
     * Converts a {@link CatalogEntity} JPA entity to a {@link Catalog} domain object.
     *
     * @param entity the JPA entity to convert
     * @return the corresponding domain object
     */
    Catalog toDomain(CatalogEntity entity);
}

