package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ThreadDetails {
    private String title;
}
