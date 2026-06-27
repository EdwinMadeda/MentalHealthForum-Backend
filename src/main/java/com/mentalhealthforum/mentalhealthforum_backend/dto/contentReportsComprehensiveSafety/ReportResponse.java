package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import java.time.Instant;
import java.util.UUID;

public interface ReportResponse {
    UUID getId();
    ReportTargetType getTargetType();
    ReportCategory getReportCategory();
    Severity getSeverity();
    ReportStatus getStatus();
    String getReason();
    String getDetails();
    String getResolutionNotes();
    ModerationAction getActionTaken();
    String getActionTakenDetails();
    DismissalReason getDismissalReason();
    Instant getReportedAt();
    Instant getLastModifiedAt();
    Boolean getIsAnonymous();

    // Reporter info
    UUID getReporterId();
    String getReporterDisplayName();
    String getReporterAvatarUrl();

    // Moderation info
    UUID getAssignedModeratorId();
    String getAssignedModeratorDisplayName();
    String getAssignedModeratorAvatarUrl();
    Instant getAssignedAt();
    Instant getReviewedAt();
    UUID getReviewedBy();
    String getReviewedByDisplayName();
    String getReviewedByAvatarUrl();
}