package com.mentalhealthforum.mentalhealthforum_backend.dto.discovery;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BookmarkResponse {

    // Bookmark metadata
    private UUID id;
    private String notes;
    private Instant bookmarkedAt;

    // Category
    private UUID categoryId;

    // Thread core info
    private UUID threadId;
    private String threadTitle;

    // Thread author
    private UUID threadCreatorId;
    private String threadCreatorDisplayName;
    private String threadCreatorAvatarUrl;

    // Thread stats
    private Integer threadPostCount;
    private Integer threadViewCount;
    private Instant threadLastActivityAt;
    private ThreadStatus threadStatus;
    private ThreadType threadType;
    private ContentWarningType contentWarningType;

}
