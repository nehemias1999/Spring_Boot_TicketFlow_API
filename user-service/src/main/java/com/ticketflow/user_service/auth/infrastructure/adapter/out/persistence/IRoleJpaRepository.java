package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RoleEntity}.
 */
public interface IRoleJpaRepository extends JpaRepository<RoleEntity, String> {
}
