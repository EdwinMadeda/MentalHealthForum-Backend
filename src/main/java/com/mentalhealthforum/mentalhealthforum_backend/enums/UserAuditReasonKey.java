package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

import java.util.Set;

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
    EXCEPTIONAL_CONTRIBUTION(UserAuditAction.PROMOTED, Set.of()),
    TRUSTED_ESTABLISHED(UserAuditAction.PROMOTED, Set.of()),
    PROFESSIONAL_CREDENTIALS(UserAuditAction.PROMOTED, Set.of(
            GroupPath.MODERATORS_PROFESSIONAL
    )),
    MODERATOR_NOMINATION(UserAuditAction.PROMOTED, Set.of(
            GroupPath.MODERATORS_PEER,
            GroupPath.MODERATORS_PROFESSIONAL
    )),
    COMMUNITY_BUILDER(UserAuditAction.PROMOTED, Set.of()),
    MENTORSHIP(UserAuditAction.PROMOTED, Set.of()),
    CONSISTENT_CONTRIBUTIONS(UserAuditAction.PROMOTED, Set.of()),
    LEADERSHIP_QUALITIES(UserAuditAction.PROMOTED, Set.of()),
    PEER_SUPPORT_EXCELLENCE(UserAuditAction.PROMOTED, Set.of()),
    SAFETY_ADVOCATE(UserAuditAction.PROMOTED, Set.of()),

    // ============================================================
    // DEMOTED - Policy and User-Initiated
    // ============================================================
    POLICY_VIOLATION(UserAuditAction.DEMOTED, Set.of()),
    INACTIVITY(UserAuditAction.DEMOTED, Set.of()),
    REQUESTED_DEMOTION(UserAuditAction.DEMOTED, Set.of()),
    CODE_OF_CONDUCT_VIOLATION(UserAuditAction.DEMOTED, Set.of()),
    ROLE_MISMATCH(UserAuditAction.DEMOTED, Set.of()),
    TEMPORARY_STEP_DOWN(UserAuditAction.DEMOTED, Set.of()),

    // ============================================================
    // DISABLED - Safety and Administrative
    // ============================================================
    TEMP_SUSPENSION(UserAuditAction.DISABLED, Set.of()),
    SECURITY_CONCERN(UserAuditAction.DISABLED, Set.of()),
    USER_REQUEST_DISABLE(UserAuditAction.DISABLED, Set.of()),
    DUPLICATE_ACCOUNT(UserAuditAction.DISABLED, Set.of()),
    SAFETY_CONCERN(UserAuditAction.DISABLED, Set.of()),
    ANONYMIZED_RETAINED(UserAuditAction.DISABLED, Set.of()),
    ADMIN_MANUAL_LOCK(UserAuditAction.DISABLED, Set.of()),

    // ============================================================
    // ENABLED - Recovery and Resolution
    // ============================================================
    ACCOUNT_RECOVERY(UserAuditAction.ENABLED, Set.of()),
    SUSPENSION_LIFTED(UserAuditAction.ENABLED, Set.of()),
    APPEAL_APPROVED(UserAuditAction.ENABLED, Set.of()),
    USER_REQUEST_ENABLE(UserAuditAction.ENABLED, Set.of()),
    READY_TO_RETURN(UserAuditAction.ENABLED, Set.of()),

    // ============================================================
    // GROUP_CHANGED - Administrative
    // ============================================================
    ADMIN_CORRECTION(UserAuditAction.GROUP_CHANGED, Set.of()),
    ROLE_REALIGNMENT(UserAuditAction.GROUP_CHANGED, Set.of()),
    SYSTEM_MIGRATION(UserAuditAction.GROUP_CHANGED, Set.of()),
    ERROR_CORRECTION(UserAuditAction.GROUP_CHANGED, Set.of()),

    // ============================================================
    // INVITE_REISSUED - Invitation Lifecycle
    // ============================================================
    INVITE_EXPIRED(UserAuditAction.INVITE_REISSUED, Set.of()),
    EMAIL_BOUNCED(UserAuditAction.INVITE_REISSUED, Set.of()),
    USER_REQUEST_RESEND(UserAuditAction.INVITE_REISSUED, Set.of()),
    ADMIN_CORRECTION_REISSUE(UserAuditAction.INVITE_REISSUED, Set.of()),
    EMAIL_UPDATED(UserAuditAction.INVITE_REISSUED, Set.of()),

    // ============================================================
    // INVITE_REVOKED - Invitation Cancellation
    // ============================================================
    INVITE_CANCELLED(UserAuditAction.INVITE_REVOKED, Set.of()),
    USER_REQUEST_REVOKE(UserAuditAction.INVITE_REVOKED, Set.of()),
    DUPLICATE_INVITE(UserAuditAction.INVITE_REVOKED, Set.of()),
    POLICY_VIOLATION_INVITE(UserAuditAction.INVITE_REVOKED, Set.of()),
    USER_NO_LONGER_INTERESTED(UserAuditAction.INVITE_REVOKED, Set.of()),
    USER_REQUEST_PAUSE(UserAuditAction.INVITE_REVOKED, Set.of()),

    // ============================================================
    // CREATED - System Action
    // ============================================================
    SYSTEM_AUTO(UserAuditAction.CREATED, Set.of());
    private final UserAuditAction actionType;
    private final Set<GroupPath> validTargetGroups;

    UserAuditReasonKey(UserAuditAction actionType, Set<GroupPath> validTargetGroups) {
        this.actionType = actionType;
        this.validTargetGroups = validTargetGroups;
    }

    /**
     * Checks if this reason is valid for given target group
     * Empty set means no constraint - valid for all groups
     * */
    public boolean isValidForGroup(GroupPath targetGroup){
        if(validTargetGroups.isEmpty()){
            return true;
        }
        return validTargetGroups.contains(targetGroup);
    }

}
