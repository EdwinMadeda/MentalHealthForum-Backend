package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

/**
 * Operation contexts for group dropdowns.
 * Each context represents a specific operation with its own set of allowed groups.
 */
public enum GroupContext {
    CREATE,  // Create new user
    REISSUE,  // Reissue invitation
    PENDING, // Update pending invite
    SYNCED // Update synced user
}
