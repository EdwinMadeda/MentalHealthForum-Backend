package com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddContentWarningRequest(
        @NotNull(message = "Content warning type is required")
        ContentWarningType contentWarningType,

        @Size(max = 100, message = "Custom warning text cannot exceed 100 characters")
        String contentWarningCustomText
) {}
