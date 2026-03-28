package com.ticketflow.ticket_service.booking.application.mapper;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
public interface ITicketApplicationMapper {

    /**
     * Converts a {@link CreateTicketRequest} DTO to a {@link Ticket} domain object.
     * <p>
     * Sets {@code deleted} to {@code false}. The {@code purchaseDate} and {@code status}
     * are set by the service layer. Timestamps are managed by JPA auditing.
     * </p>
     *
     * @param request the creation request DTO
     * @return a new {@link Ticket} domain object ready for business logic processing
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "purchaseDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ticket toDomain(CreateTicketRequest request);

    /**
     * Converts a {@link Ticket} domain object to a {@link TicketResponse} DTO.
     *
     * @param ticket the domain object
     * @return a response DTO suitable for API output
     */
    TicketResponse toResponse(Ticket ticket);

}
