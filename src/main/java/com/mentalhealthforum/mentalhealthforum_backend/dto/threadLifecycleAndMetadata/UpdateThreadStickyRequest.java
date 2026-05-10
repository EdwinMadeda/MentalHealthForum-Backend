package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import jakarta.validation.constraints.NotNull;

public record UpdateThreadStickyRequest(
        @NotNull(message = "Is Sticky is required")
        boolean isSticky
) {}
