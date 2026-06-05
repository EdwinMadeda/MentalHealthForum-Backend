package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.userStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatusResponse {
    private boolean canAccess;
    private String message; // Backend generates user friendly message
    private String redirectUrl;

    // Raw data (optional - for fronted that wants to customize)
    private RestrictionDetails restriction;

    @Data
    @Builder
    public static class RestrictionDetails{
        private String type;  // BANNED, SUSPENDED, MUTED, ONBOARDING
        private String reason;
        private String expiresAt;
        private String humanReadableExpiry;
        private String imposedBy;
    }
}
