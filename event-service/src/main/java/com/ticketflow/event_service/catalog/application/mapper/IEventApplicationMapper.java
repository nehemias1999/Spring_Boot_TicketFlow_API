package com.ticketflow.event_service.catalog.application.mapper;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.domain.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between application-layer DTOs and the domain model.
 * <p>
 * Spring manages this mapper as a bean thanks to the {@code componentModel = "spring"}
 * configuration. The implementation is generated at compile time by the MapStruct
 * annotation processor.
 * </p>
 *
 * @author TicketFlow Team
 */
@Mapper(componentModel = "spring")
public interface IEventApplicationMapper {

    /**
     * Converts a {@link CreateEventRequest} DTO to a {@link Event} domain object.
     * <p>
     * Sets {@code deleted} to {@code false}. Timestamps ({@code createdAt} and
     * {@code updatedAt}) are managed automatically by JPA auditing.
     * </p>
     *
     * @param request the creation request DTO
     * @return a new {@link Event} domain object ready to be persisted
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toDomain(CreateEventRequest request);

    /**
     * Converts a {@link Event} domain object to a {@link EventResponse} DTO.
     *
     * @param event the domain object
     * @return a response DTO suitable for API output
     */
    EventResponse toResponse(Event event);

    /**
     * Applies the fields from an {@link UpdateEventRequest} onto an existing
     * {@link Event} domain object, preserving its ID, creation timestamp,
     * and deleted status. Updates the {@code updatedAt} timestamp automatically.
     *
     * @param request the update request DTO with new field values
     * @param event the existing event domain object to update in place
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateDomainFromRequest(UpdateEventRequest request, @MappingTarget Event event);
}
