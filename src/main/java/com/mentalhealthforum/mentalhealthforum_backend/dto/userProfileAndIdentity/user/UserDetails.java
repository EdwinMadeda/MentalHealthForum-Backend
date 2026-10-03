package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDetails {
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private String initials;
    private Instant lastActiveAt; // TODO: Implement activity tracking (sprint backlog)
}
