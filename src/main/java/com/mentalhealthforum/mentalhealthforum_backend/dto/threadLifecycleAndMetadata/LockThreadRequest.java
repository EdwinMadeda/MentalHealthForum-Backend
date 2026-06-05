package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record   LockThreadRequest(
        @NotBlank(message = "Lock reason is required")
        @Size(min = 5, max = 500, message = "Lock reason must be between 5 and 500 characters")
        String reason,

        @Positive(message = "Duration must be positive")
        Integer durationHours  // Optional - if null, permanent lock
) {}
