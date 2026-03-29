package com.ticketflow.user_service.auth.application.mapper;

import com.ticketflow.user_service.auth.application.dto.request.RegisterRequest;
import com.ticketflow.user_service.auth.application.dto.response.UserResponse;
import com.ticketflow.user_service.auth.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between application-layer DTOs and the domain model.
 *
 * @author TicketFlow Team
 */
@Mapper(componentModel = "spring")
public interface IUserApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toDomain(RegisterRequest request);

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);
}
