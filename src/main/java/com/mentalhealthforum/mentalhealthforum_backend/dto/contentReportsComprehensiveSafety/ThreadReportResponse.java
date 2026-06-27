package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ThreadReportResponse implements ReportResponse {
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
    private UUID reporterId;
    private String reporterDisplayName;
    private String reporterAvatarUrl;

    // Thread-specific fields (REQUIRED)
    private UUID threadId;
    private String threadTitle;

    // Moderation info
    private UUID assignedModeratorId;
    private String assignedModeratorDisplayName;
    private String assignedModeratorAvatarUrl;
    private Instant assignedAt;
    private Instant reviewedAt;
    private UUID reviewedBy;
    private String reviewedByDisplayName;
    private String reviewedByAvatarUrl;
}