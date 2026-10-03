package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for pending admin invitations.
 */
@Data
@Builder
public class PendingAdminInviteResponse {
    // Primary key used by the application
    private UUID userId;

    // Basic Profile Information
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    private GroupPath[] groups;

    // Status and Audit Fields
    private boolean isEnabled;
    private boolean isEmailVerified;

    // Invited by Details (single object)
    private UserDetails invitedBy;

    private Instant dateCreated;
    private Instant updatedAt;

    private OnboardingStage currentStage;

    // Expiry fields
    private Instant expiresAt;
    private boolean isExpired;
    private boolean isEligibleForPurge;

};