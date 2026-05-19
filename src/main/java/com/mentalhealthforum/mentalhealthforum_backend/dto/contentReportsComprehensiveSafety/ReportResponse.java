package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
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
public class    ReportResponse {

    private UUID id;
    private UUID reporterId;
    private boolean isAnonymous;
    private ReportTargetType targetType;
    private UUID threadId;
    private UUID postId;
    private UUID reportedUserId;
    private ReportCategory reportCategory;
    private Severity severity;
    private String reason;
    private String details;
    private ReportStatus status;
    private UUID assignedModeratorId;
    private Instant assignedAt;
    private ModerationAction actionTaken;
    private String actionTakenDetails;
    private String resolutionNotes;
    private DismissalReason dismissalReason;
    private Instant reviewedAt;
    private UUID reviewedBy;
    private Instant reportedAt;
    private Instant lastModifiedAt;

}
