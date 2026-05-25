package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MergeThreadRequest(
    @NotNull(message = "Source thread ID is required")
    UUID sourceThreadId,

    @NotNull(message = "Destination thread ID  is required")
    UUID destinationThreadId
) {}
