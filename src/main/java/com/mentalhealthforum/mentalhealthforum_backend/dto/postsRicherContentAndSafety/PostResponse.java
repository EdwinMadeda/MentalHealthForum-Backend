package com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety;

import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.EditReason;
import com.mentalhealthforum.mentalhealthforum_backend.enums.PostType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private UUID id;

    // Reference (nested)
    private ThreadDetails thread;
    private PostDetails parentPost;

    // Reference (nested)
    private UserDetails author;
    private String anonymousIdentifier;

    private PostType postType;
    private String content;
    private Integer wordCount;

    private ContentWarningType contentWarningType;
    private String contentWarningCustomText;

    private boolean isFlaggedForReview;
    private boolean isEdited;
    private EditReason editReason;
    private String editReasonCustomText;
    private Instant editedAt;
    // Reference (nested)
    private UserDetails editedBy;


    private boolean isAnonymous;
    private boolean isDeleted;

    private Integer reactionCount;

    private Instant createdAt;
    private Instant updatedAt;

}
