package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapped to the {@code roles} table.
 * <p>
 * Acts as the authoritative catalog of valid role names.
 * Both {@code users.role} and {@code role_permissions.role} reference
 * this table via foreign-key constraints.
 * </p>
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description", length = 200)
    private String description;
}
