package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MfaConfirmResponse(
      boolean enabled,
      List<String> backupCodes
) {}
