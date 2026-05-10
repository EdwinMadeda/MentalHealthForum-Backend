package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import jakarta.validation.constraints.NotNull;

public record UpdateThreadFeaturedRequest(
        @NotNull(message = "Is Featured is required")
        boolean isFeatured
) {
}
