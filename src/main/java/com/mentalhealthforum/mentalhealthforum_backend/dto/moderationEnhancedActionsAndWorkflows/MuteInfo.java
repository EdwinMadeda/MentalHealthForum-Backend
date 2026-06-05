package com.mentalhealthforum.mentalhealthforum_backend.dto.moderationEnhancedActionsAndWorkflows;

import java.time.Instant;
import java.util.UUID;

public class MuteInfo {
    private boolean isMuted;
    private Instant expiresAt;
    private String reason;
    private UUID imposedBy;
    private String imposedByDisplayName;

}
