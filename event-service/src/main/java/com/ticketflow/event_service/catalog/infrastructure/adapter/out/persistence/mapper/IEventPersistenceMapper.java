package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper;

import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.EventEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between the JPA {@link EventEntity}
 * and the domain {@link Event} model.
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
public interface IEventPersistenceMapper {

    /**
     * Converts a {@link Event} domain object to a {@link EventEntity} JPA entity.
     *
     * @param event the domain object to convert
     * @return the corresponding JPA entity
     */
    EventEntity toEntity(Event event);

    /**
     * Converts a {@link EventEntity} JPA entity to a {@link Event} domain object.
     *
     * @param entity the JPA entity to convert
     * @return the corresponding domain object
     */
    Event toDomain(EventEntity entity);
}

