package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryTagDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryTagResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadResponse {
    private UUID id;

    // References nested
    private CategoryDetails category;
    private List<CategoryTagDetails> categoryTags;
    private UserDetails creator;

    // Thread subject attributes
    private String title;
    private ThreadType threadType;
    private ThreadStatus threadStatus;
    private ContentWarningType contentWarningType;
    private String contentWarningCustomText;

    private boolean isSticky;
    private boolean isFeatured;
    private boolean isBookmarked;
    private boolean isWatched;

    private Integer bookmarkCount;
    private Integer postCount;
    private Integer viewCount;

    private UUID bestAnswerPostId;

    // User references (nested
    private UserDetails resolvedBy;  // ResolvedBy reference
    private Instant resolvedAt;

    // Lock metadata
    private String lockReason;
    private UserDetails lockedBy;     // LockedBy reference
    private Instant lockedAt;
    private Instant lockExpiresAt;

    // Edit metadata
    private Instant lastEditedAt;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActivityAt;

}
