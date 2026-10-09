package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
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

    Instant getLastModifiedAt();
    Boolean getIsAnonymous();

    // Reporter info
    UserDetails getReporter();
    Instant getReportedAt();

    // Moderation info
    UserDetails getAssignedModerator();
    UserDetails getReviewer();

    Instant getAssignedAt();
    Instant getReviewedAt();

}