package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import jakarta.validation.constraints.NotNull;

public record UpdateThreadTypeRequest(
        @NotNull(message = "Thread type is required")
        ThreadType threadType
) {}
