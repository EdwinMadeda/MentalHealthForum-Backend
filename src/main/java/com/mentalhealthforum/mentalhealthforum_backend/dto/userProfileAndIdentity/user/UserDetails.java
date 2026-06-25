package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserDetails {
    private String displayName;
    private String avatarUrl;
    private String bio;
    private Instant lastActiveAt; // TODO: Implement activity tracking (sprint backlog)
}
