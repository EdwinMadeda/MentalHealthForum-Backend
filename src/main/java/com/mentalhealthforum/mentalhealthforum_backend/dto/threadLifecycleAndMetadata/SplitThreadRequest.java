package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SplitThreadRequest(
        @NotEmpty(message = "At least one post must be selected to split")
        List<UUID> postIds,

        @NotBlank(message = "New thread title is required")
        @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
        String newThreadTitle
) {}
