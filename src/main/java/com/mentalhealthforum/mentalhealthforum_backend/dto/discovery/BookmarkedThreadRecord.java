package com.mentalhealthforum.mentalhealthforum_backend.dto.discovery;

import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;

import java.time.Instant;
import java.util.UUID;

public record BookmarkedThreadRecord(
    UUID bookmark_id,
    UUID thread_id,
    String title,
    UUID category_id,
    UUID creator_id,
    Integer post_count,
    Integer view_count,
    Instant last_activity_at,
    String thread_status,
    String thread_type,
    String content_warning_type,
    Instant bookmarked_at,
    String bookmark_notes
) {
    public ThreadDetails toThreadDetails(){
        return ThreadDetails.builder()
                .id(thread_id)
                .title(title)
                .threadType(ThreadType.fromString(thread_type))
                .threadStatus(ThreadStatus.fromString(thread_status))
                .postCount(post_count)
                .viewCount(view_count)
                .lastActivityAt(last_activity_at)
                .contentWarningType(ContentWarningType.fromString(content_warning_type))
                .build();
    }
}
