package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserReportResponse implements ReportResponse {
    // Common report fields
    private UUID id;
    private ReportTargetType targetType;
    private ReportCategory reportCategory;
    private Severity severity;
    private String reason;
    private String details;
    private ReportStatus status;
    private String resolutionNotes;
    private ModerationAction actionTaken;
    private String actionTakenDetails;
    private DismissalReason dismissalReason;
    private Instant reportedAt;
    private Instant lastModifiedAt;
    private Boolean isAnonymous;

    // Reporter info
    private UserDetails reporter;

    // User-specific fields (REQUIRED)
    private UserDetails reportedUser;

    // Moderation info
    private UserDetails assignedModerator;
    private UserDetails reviewer;

    private Instant assignedAt;
    private Instant reviewedAt;

}