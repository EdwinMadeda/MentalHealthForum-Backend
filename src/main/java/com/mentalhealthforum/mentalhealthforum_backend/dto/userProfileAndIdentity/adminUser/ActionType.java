package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

public enum ActionType {
    PROMOTE, // Moving up the hierarchy
    DEMOTE, // Moving down the hierarchy
    CORRECT, // Fixing an improper assignment
    SAME     // Current group (no change)
}
