package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ThreadDetails {
    private String title;
    private UUID categoryId;
}
