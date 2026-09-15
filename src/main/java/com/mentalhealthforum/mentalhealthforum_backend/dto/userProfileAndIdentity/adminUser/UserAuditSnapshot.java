package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;

/**
 * Strongly typed snapshot of user fields for audit logging.
 * Only includes fields that are tracked for audit purposes.
 *
 * <p><b>Note:</b> Email is intentionally not tracked to avoid storing PII
 * in the audit log, which would conflict with anonymization and GDPR compliance.
 *
 * <p>Fields are nullable - only the changed field(s) are populated:
 *
 * <p>For multiple simultaneous changes, use the full constructor:
 * {@code new UserAuditSnapshot(group, isEnabled, stage)}
 */
public record UserAuditSnapshot(
        GroupPath group,    // Current group
        Boolean isEnabled  // Enabled/disabled status
) {
    // Default empty snapshot (all nulls)
    public UserAuditSnapshot() {
        this(null, null);
    }

    // Convenience factory for single-field changes (common)
    public static UserAuditSnapshot forGroup(GroupPath group){
        return new UserAuditSnapshot(group, null);
    }

    public static UserAuditSnapshot forEnabled(Boolean isEnabled){
        return new UserAuditSnapshot(null, isEnabled);
    }

    // For multi-field changes, use the full constructor
    // new UserAuditSnapshot(newGroup, false)

    // Check if snapshot is empty (no changes tracked)
    @JsonIgnore  // ← Tell Jackson to ignore this method
    public boolean isEmpty(){
        return group == null && isEnabled == null;
    }
}
