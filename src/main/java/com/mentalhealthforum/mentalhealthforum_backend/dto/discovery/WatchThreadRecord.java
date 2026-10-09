package com.mentalhealthforum.mentalhealthforum_backend.dto.discovery;

import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;

import java.time.Instant;
import java.util.UUID;

public record WatchThreadRecord(
    // Watch metadata
    UUID watch_id,
    Boolean notification_enabled,
    Instant watched_at,

    // Thread core info
    UUID thread_id,
    String thread_title,
    UUID creator_id,
    UUID category_id,

    // Thread type and status
    String thread_type,
    String thread_status,
    String content_warning_type,

    // Thread stats
    Integer post_count,
    Integer view_count,
    Instant last_activity_at,

    // Thread flags
    Boolean is_sticky,
    Boolean is_featured

) {
    public ThreadDetails toThreadDetails(){
        return ThreadDetails.builder()
                .id(thread_id)
                .title(thread_title)
                .threadType(ThreadType.fromString(thread_type))
                .threadStatus(ThreadStatus.fromString(thread_status))
                .postCount(post_count)
                .viewCount(view_count)
                .lastActivityAt(last_activity_at)
                .contentWarningType(ContentWarningType.fromString(content_warning_type))
                .isSticky(is_sticky)
                .isFeatured(is_featured)
                .build();
    }

}
