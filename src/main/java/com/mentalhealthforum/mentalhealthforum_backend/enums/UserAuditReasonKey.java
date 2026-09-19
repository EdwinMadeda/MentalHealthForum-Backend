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
    // ============================================================
    // PROMOTED - Trusted Community Members
    // ============================================================
    EXCEPTIONAL_CONTRIBUTION(UserAuditAction.PROMOTED),
    TRUSTED_ESTABLISHED(UserAuditAction.PROMOTED),
    PROFESSIONAL_CREDENTIALS(UserAuditAction.PROMOTED),
    MODERATOR_NOMINATION(UserAuditAction.PROMOTED),
    COMMUNITY_BUILDER(UserAuditAction.PROMOTED),
    MENTORSHIP(UserAuditAction.PROMOTED),
    CONSISTENT_CONTRIBUTIONS(UserAuditAction.PROMOTED),
    LEADERSHIP_QUALITIES(UserAuditAction.PROMOTED),
    PEER_SUPPORT_EXCELLENCE(UserAuditAction.PROMOTED),
    SAFETY_ADVOCATE(UserAuditAction.PROMOTED),

    // ============================================================
    // DEMOTED - Policy and User-Initiated
    // ============================================================
    POLICY_VIOLATION(UserAuditAction.DEMOTED),
    INACTIVITY(UserAuditAction.DEMOTED),
    REQUESTED_DEMOTION(UserAuditAction.DEMOTED),
    CODE_OF_CONDUCT_VIOLATION(UserAuditAction.DEMOTED),
    ROLE_MISMATCH(UserAuditAction.DEMOTED),
    TEMPORARY_STEP_DOWN(UserAuditAction.DEMOTED),

    // ============================================================
    // DISABLED - Safety and Administrative
    // ============================================================
    TEMP_SUSPENSION(UserAuditAction.DISABLED),
    SECURITY_CONCERN(UserAuditAction.DISABLED),
    USER_REQUEST_DISABLE(UserAuditAction.DISABLED),
    DUPLICATE_ACCOUNT(UserAuditAction.DISABLED),
    SAFETY_CONCERN(UserAuditAction.DISABLED),
    ANONYMIZED_RETAINED(UserAuditAction.DISABLED),
    ADMIN_MANUAL_LOCK(UserAuditAction.DISABLED),

    // ============================================================
    // ENABLED - Recovery and Resolution
    // ============================================================
    ACCOUNT_RECOVERY(UserAuditAction.ENABLED),
    SUSPENSION_LIFTED(UserAuditAction.ENABLED),
    APPEAL_APPROVED(UserAuditAction.ENABLED),
    USER_REQUEST_ENABLE(UserAuditAction.ENABLED),
    READY_TO_RETURN(UserAuditAction.ENABLED),

    // ============================================================
    // GROUP_CHANGED - Administrative
    // ============================================================
    ADMIN_CORRECTION(UserAuditAction.GROUP_CHANGED),
    ROLE_REALIGNMENT(UserAuditAction.GROUP_CHANGED),
    SYSTEM_MIGRATION(UserAuditAction.GROUP_CHANGED),
    ERROR_CORRECTION(UserAuditAction.GROUP_CHANGED),

    // ============================================================
    // INVITE_REISSUED - Invitation Lifecycle
    // ============================================================
    INVITE_EXPIRED(UserAuditAction.INVITE_REISSUED),
    EMAIL_BOUNCED(UserAuditAction.INVITE_REISSUED),
    USER_REQUEST_RESEND(UserAuditAction.INVITE_REISSUED),
    ADMIN_CORRECTION_REISSUE(UserAuditAction.INVITE_REISSUED),
    EMAIL_UPDATED(UserAuditAction.INVITE_REISSUED),

    // ============================================================
    // INVITE_REVOKED - Invitation Cancellation
    // ============================================================
    INVITE_CANCELLED(UserAuditAction.INVITE_REVOKED),
    USER_REQUEST_REVOKE(UserAuditAction.INVITE_REVOKED),
    DUPLICATE_INVITE(UserAuditAction.INVITE_REVOKED),
    POLICY_VIOLATION_INVITE(UserAuditAction.INVITE_REVOKED),
    USER_NO_LONGER_INTERESTED(UserAuditAction.INVITE_REVOKED),
    USER_REQUEST_PAUSE(UserAuditAction.INVITE_REVOKED),

    // ============================================================
    // CREATED - System Action
    // ============================================================
    SYSTEM_AUTO(UserAuditAction.CREATED);

    private final UserAuditAction actionType;

    UserAuditReasonKey(UserAuditAction actionType) {
        this.actionType = actionType;
    }

}
