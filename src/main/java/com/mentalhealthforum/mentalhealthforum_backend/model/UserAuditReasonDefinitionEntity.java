package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity representing the predefined reason definitions for audit logs.
 *
 * <p>These definitions are seeded via database migration and provide:
 * <ul>
 *   <li>Predefined reasons for admin selection (e.g., "Exceptional contribution")</li>
 *   <li>Action type mapping (e.g., PROMOTED, DEMOTED)</li>
 *   <li>Display ordering for UI dropdowns</li>
 * </ul>
 *
 * <p><b>Note:</b> Admins can also provide a custom reason in addition to
 * or instead of a suggested reason.
 */

@Getter
@Setter
@NoArgsConstructor
@Table("user_audit_reason_definitions")
public class UserAuditReasonDefinitionEntity {
    @Id
    private UUID id; // DB-generated UUID Primary Key

    @Column("key")
    private UserAuditReasonKey key; // EXCEPTIONAL_CONTRIBUTION, etc.

    @Column("description")
    private String description; // Human-readable description

    @Column("action_type")
    private UserAuditAction actionType; // PROMOTED, DEMOTED, etc.

    @Column("is_active")
    private Boolean isActive = true; // Soft delete flag

    @Column("sort_order")
    private Integer sortOrder = 0; // UI ordering

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

}
