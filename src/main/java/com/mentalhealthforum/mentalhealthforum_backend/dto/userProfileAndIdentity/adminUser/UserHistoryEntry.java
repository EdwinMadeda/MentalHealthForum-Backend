package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a single audit log entry for a user.
 *
 * <p>This is the read model for the frontend, containing:
 * <ul>
 *   <li>What action was performed</li>
 *   <li>Previous and new values (raw and display-friendly)</li>
 *   <li>Who performed it</li>
 *   <li>Optional reason (suggested from definitions + custom)</li>
 *   <li>When it happened</li>
 * </ul>
 *
 * <p><b>Note:</b> The suggested reason comes from {@code user_audit_reason_definitions}
 * and is nullable. The custom reason is admin-entered and is also nullable.
 * Both can be present simultaneously.
 */
public record UserHistoryEntry(
        UserAuditAction action,                 // PROMOTION, DEMOTION, etc
        UserAuditSnapshot oldValue,             // Raw value (e.g., "/members/new")
        UserAuditSnapshot newValue,             // Raw value (e.g., "/moderators/peer")
        UUID performedById,                     // Keycloak ID of performer (for linking)
        String performedByDisplayName,          // Display name of performer
        UserAuditReasonDefinitionDto suggestedReason,  // From definitions
        String customReason,                    // Admin's own reason
        Instant timeStamp
) {}
