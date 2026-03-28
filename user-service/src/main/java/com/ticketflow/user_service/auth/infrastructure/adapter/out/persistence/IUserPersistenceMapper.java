package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import com.ticketflow.user_service.auth.domain.model.User;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between the JPA {@link UserEntity}
 * and the domain {@link User} model.
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
public interface IUserPersistenceMapper {

    /**
     * Converts a {@link User} domain object to a {@link UserEntity} JPA entity.
     *
     * @param user the domain object to convert
     * @return the corresponding JPA entity
     */
    UserEntity toEntity(User user);

    /**
     * Converts a {@link UserEntity} JPA entity to a {@link User} domain object.
     *
     * @param entity the JPA entity to convert
     * @return the corresponding domain object
     */
    User toDomain(UserEntity entity);
}
