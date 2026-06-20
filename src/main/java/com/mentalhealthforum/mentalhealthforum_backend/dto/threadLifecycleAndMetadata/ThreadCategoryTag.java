package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ThreadCategoryTag{
    private UUID id;
    private String name;
    private String slug;;
}