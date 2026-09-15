package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

/**
 * Defines the predefined reason keys for audit log entries.
 *
 * <p>These keys map to records in {@code user_audit_reason_definitions}.
 * Each key has a corresponding display description and action type.
 *
 * <p><b>Note:</b> Admins can also provide a custom reason in addition to
 * or instead of a suggested reason.
 */

@Getter
public enum UserAuditReasonKey {
    // Promotions
    EXCEPTIONAL_CONTRIBUTION(UserAuditAction.PROMOTED),
    TRUSTED_ESTABLISHED(UserAuditAction.PROMOTED),
    PROFESSIONAL_CREDENTIALS(UserAuditAction.PROMOTED),
    MODERATOR_NOMINATION(UserAuditAction.PROMOTED),

    // Demotions
    POLICY_VIOLATION(UserAuditAction.DEMOTED),
    INACTIVITY(UserAuditAction.DEMOTED),
    REQUESTED_DEMOTION(UserAuditAction.DEMOTED),

    // Disable/Enable
    TEMP_SUSPENSION(UserAuditAction.DISABLED),
    ACCOUNT_RECOVERY(UserAuditAction.ENABLED),

    // General
    ADMIN_CORRECTION(UserAuditAction.GROUP_CHANGED),
    SYSTEM_AUTO(UserAuditAction.CREATED);

    private final UserAuditAction actionType;

    UserAuditReasonKey(UserAuditAction actionType) {
        this.actionType = actionType;
    }

}
