package com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.PostType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PostDetails {
    private UUID id;
    private String content;
    private PostType postType;
//    private UserDetails author;
    private Boolean isDeleted;
    private Boolean isAnonymous;
    private String anonymousIdentifier;
    private Instant createdAt;
}
