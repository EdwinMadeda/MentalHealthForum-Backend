package com.mentalhealthforum.mentalhealthforum_backend.enums;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.GroupContext;
import lombok.Getter;

/**
 * Defines all admin-initiated actions that can be logged in the user audit log.
 *
 * <p>This enum is used for:
 * <ul>
 *   <li>Audit log entries ({@code user_audit_log.action_type})</li>
 *   <li>Filtering history by action type</li>
 *   <li>Frontend display and icons</li>
 * </ul>
 *
 * <p><b>Note:</b> Only admin-initiated changes are tracked. User-initiated changes
 * (login, password change, profile update) are not logged here.
 */
@Getter
public enum UserAuditAction {
    // User lifecycle
    CREATED("Created"),           // Admin created user
    SYNCED("Synced"),           // User synced to app_users (pending → synced)

    // Group changes (synced users only)
    PROMOTED("Promoted"),          // Moved to higher group
    DEMOTED("Demoted"),          // Moved to lower group

    // Group changes (pending users only)
    GROUP_CHANGED("Group Changed"),     // Changed between pending groups (MEMBERS_NEW ↔ MODERATORS_PROFESSIONAL or correction whilst still pending)

    // Account status
    ENABLED("Enabled"),           // Account enabled
    DISABLED("Disabled"),          // Account disabled

    // Invitation lifecycle
    INVITE_REISSUED("Invitation Reissued"),   // Invitation reissued
    INVITE_REVOKED("Invitation Revoked");    // Invitation revoked

    private  final String displayName;

    UserAuditAction(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Determines the UserAuditAction for a group change.
     */
    public static UserAuditAction forGroupChange(GroupContext context, GroupPath currentGroup, GroupPath targetGroup){

        if(context == GroupContext.REISSUE){
            // Reissue primary action is INVITE_REISSUED
            return INVITE_REISSUED;
        }

        if(context == GroupContext.PENDING){
            return GROUP_CHANGED;
            // Pending update primary action is GROUP_CHANGED
        }

        if(context == GroupContext.SYNCED){
            // Synced users use PROMOTED/DEMOTED
            if (GroupPath.isPromotion(currentGroup, targetGroup)) {
                return PROMOTED;
            }
            else if (GroupPath.isDemotion(currentGroup, targetGroup)) {
                return DEMOTED;
            }
            return GROUP_CHANGED;

        }

        // CREATE context - no reason needed
        return null;

    }

    /**
     * Determines the UserAuditAction for  enabled change.
     */
    public static UserAuditAction forEnabledChange(Boolean isEnabled){
        return isEnabled ? ENABLED : DISABLED;
    }
}
