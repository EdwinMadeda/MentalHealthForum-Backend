package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for pending admin invitations.
 *
 * <p>Implements {@link InviteExpirable} to provide:
 * <ul>
 *   <li>{@code is_expired} - Whether the invitation has passed its expiry time</li>
 *   <li>{@code is_eligible_for_purge} - Whether the invitation is eligible for purge</li>
 * </ul>
 */
public record PendingAdminInviteDto(
        // Primary key used by the application
        UUID user_id,

        // Basic Profile Information
        String username,
        String first_name,
        String last_name,
        String email,

        String[] groups,

        // Status and Audit Fields
        boolean is_enabled,
        boolean is_email_verified,

        // Invited by Details
        UUID invited_by,
        String invited_by_display_name,
        String invited_by_avatar_url,

        Instant date_created,
        Instant updated_at,

        OnboardingStage current_stage,

        // Expiry fields
        Instant expires_at
) implements InviteExpirable {
    @Override
    public Instant getExpiresAt() {
        return this.expires_at;
    }

    @Override
    public boolean getIsEmailVerified() {return this.is_email_verified;}

    @Override
    public OnboardingStage getCurrentStage() {
        return this.current_stage;
    }

}