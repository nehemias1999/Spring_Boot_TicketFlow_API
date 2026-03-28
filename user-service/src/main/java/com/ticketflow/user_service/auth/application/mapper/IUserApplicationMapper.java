package com.ticketflow.user_service.auth.application.mapper;

import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.UserResponse;
import com.ticketflow.user_service.auth.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between application-layer DTOs and the domain model.
 * <p>
 * Spring manages this mapper as a bean thanks to the {@code componentModel = "spring"}
 * configuration set globally via the compiler argument. The implementation is generated
 * at compile time by the MapStruct annotation processor.
 * </p>
 *
 * @author TicketFlow Team
 */
@Mapper(componentModel = "spring")
public interface IUserApplicationMapper {

    /**
     * Converts a {@link RegisterRequest} DTO to a partial {@link User} domain object.
     * <p>
     * Only the {@code email} field is mapped. The {@code id}, {@code password},
     * {@code role}, {@code deleted}, {@code createdAt}, and {@code updatedAt} fields
     * are intentionally ignored here; they are set explicitly by the service layer
     * after hashing and ID generation.
     * </p>
     *
     * @param request the registration request DTO
     * @return a partially populated {@link User} domain object
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toDomain(RegisterRequest request);

    /**
     * Converts a {@link User} domain object to a {@link UserResponse} DTO.
     * <p>
     * The {@code role} field is mapped from the enum to its string name representation.
     * </p>
     *
     * @param user the domain object to convert
     * @return a response DTO with the user's non-sensitive profile data
     */
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);
}
