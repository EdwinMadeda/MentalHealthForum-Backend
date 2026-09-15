package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.UserAuditSnapshot;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.utils.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity representing the audit log for admin-initiated user changes.
 *
 * <p>Each record captures:
 * <ul>
 *   <li>Who was changed ({@code user_id})</li>
 *   <li>What action was performed ({@code action_type})</li>
 *   <li>Previous and new values ({@code old_value}, {@code new_value})</li>
 *   <li>Who performed it ({@code performed_by})</li>
 *   <li>Optional reason ({@code reason_definition_id}, {@code custom_reason})</li>
 * </ul>
 *
 * <p><b>Note:</b> This log is append-only. Undo/Redo is achieved by adding
 * a new entry with reversed values, not by modifying existing entries.
 */
@Getter
@Setter
@NoArgsConstructor
@Table("user_audit_log")
public class UserAuditLogEntity {

    @Id
    @Column("id")
    private UUID id; // DB-generated UUID Primary Key

    @Column("user_id")
    private UUID userId; // The user being tracked (Keycloak ID)

    @Column("action_type")
    private UserAuditAction actionType; // PROMOTED, DEMOTED, etc

    @Column("old_value")
    private JsonNode oldValueJson; // Previous state (e.g., {"group": "/members/new"})

    @Column("new_value")
    private JsonNode newValueJson; // New state (e.g., {"group": "/moderators/peer"})

    @Column("performed_by")
    private UUID performedBy; // Keycloak ID of the admin who performed the action

    @Column("reason_definition_id")
    private UUID reasonDefinitionId; // FK to user_audit_reason_definitions (nullable)

    @Column("custom_reason")
    private String customReason;

    @Column("created_at")
    private Instant createdAt;

    public UserAuditLogEntity(
            UUID userId,
            UserAuditAction actionType,
            UserAuditSnapshot oldValue,
            UserAuditSnapshot newValue,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        this.userId = userId;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.reasonDefinitionId = reasonDefinitionId;
        this.customReason = customReason;
        this.createdAt = Instant.now();

        setOldValue(oldValue);
        setNewValue(newValue);
    }

    // --- UserAuditSnapshot getter/setter using JsonUtils ---

    public UserAuditSnapshot getOldValue() {
        if(oldValueJson == null || oldValueJson.isEmpty()){
            return new UserAuditSnapshot();
        }
        return JsonUtils.jsonNodeToObject(oldValueJson, UserAuditSnapshot.class);
    }

    public void setOldValue(UserAuditSnapshot snapshot){
        this.oldValueJson = snapshot == null? null: JsonUtils.objectToJsonNode(snapshot);
    }

    public UserAuditSnapshot getNewValue() {
        if(newValueJson == null || newValueJson.isEmpty()){
            return new UserAuditSnapshot();
        }
        return JsonUtils.jsonNodeToObject(newValueJson, UserAuditSnapshot.class);
    }

    public void setNewValue(UserAuditSnapshot snapshot){
        this.newValueJson = snapshot == null? null: JsonUtils.objectToJsonNode(snapshot);
    }

}
