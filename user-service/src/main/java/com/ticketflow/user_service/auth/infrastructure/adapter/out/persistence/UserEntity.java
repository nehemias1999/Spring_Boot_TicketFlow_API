package com.ticketflow.user_service.auth.infrastructure.adapter.out.persistence;

import com.ticketflow.user_service.auth.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA entity mapped to the {@code users} table in the database.
 * <p>
 * This class is an infrastructure concern and should not leak into
 * the domain or application layers. Conversion to/from the domain
 * model is handled by {@link IUserPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /**
     * Unique identifier for the user record, stored as a UUID string.
     */
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * The user's email address. Used as the unique login credential.
     */
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /**
     * The user's BCrypt-hashed password.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * The user's role, stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * Soft-delete flag. {@code true} means the record is logically deleted.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp when the record was created. Populated automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated. Populated automatically by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
