package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;

import java.time.Instant;
import java.util.UUID;

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

        OnboardingStage current_stage
) {}