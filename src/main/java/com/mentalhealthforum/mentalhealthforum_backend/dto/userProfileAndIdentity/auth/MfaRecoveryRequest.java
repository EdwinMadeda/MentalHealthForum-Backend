package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MfaRecoveryRequest(
        @NotBlank(message = "Backup code is required")
        @Size(min = 8, max = 8, message = "Backup code must be 8 digits")
        String backupCode
) {}
