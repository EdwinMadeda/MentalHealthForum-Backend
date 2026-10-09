package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ThreadDetails {
    // Thread core info
    private UUID id;
    private String title;
    private ThreadType threadType;
    private ThreadStatus threadStatus;

    // Quick context (most valuable for users)
    private Integer postCount;
    private Integer viewCount;
    private Instant lastActivityAt;

    // Content warnings
    private ContentWarningType contentWarningType;

    // Thread flags
    private Boolean isSticky;
    private Boolean isFeatured;

    // Thread settings
//    private Boolean isOpen; // PRIVACY/DESIGN: deferred to projection sweep
}
