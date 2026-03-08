package com.ticketflow.event_service.catalog.application.mapper;

import com.ticketflow.event_service.catalog.application.dto.response.CatalogResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateCatalogRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateCatalogRequest;
import com.ticketflow.event_service.catalog.domain.model.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

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
@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface ICatalogApplicationMapper {

    /**
     * Converts a {@link CreateCatalogRequest} DTO to a {@link Catalog} domain object.
     * <p>
     * Sets {@code deleted} to {@code false}, initializes {@code createdAt}
     * to the current timestamp, and leaves {@code updatedAt} as {@code null}
     * since the entry has not been updated yet.
     * </p>
     *
     * @param request the creation request DTO
     * @return a new {@link Catalog} domain object ready to be persisted
     */
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Catalog toDomain(CreateCatalogRequest request);

    /**
     * Converts a {@link Catalog} domain object to a {@link CatalogResponse} DTO.
     *
     * @param catalog the domain object
     * @return a response DTO suitable for API output
     */
    CatalogResponse toResponse(Catalog catalog);

    /**
     * Applies the fields from an {@link UpdateCatalogRequest} onto an existing
     * {@link Catalog} domain object, preserving its ID, creation timestamp,
     * and deleted status. Updates the {@code updatedAt} timestamp automatically.
     *
     * @param request the update request DTO with new field values
     * @param catalog the existing catalog domain object to update in place
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void updateDomainFromRequest(UpdateCatalogRequest request, @MappingTarget Catalog catalog);
}
