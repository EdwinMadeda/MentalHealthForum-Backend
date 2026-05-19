package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("content_reports")
public class ContentReportEntity {

    @Id
    @Column
    private UUID id;

    @Column("reporter_id")
    private UUID reporterId;

    @Column("is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    @Column("target_type")
    private ReportTargetType targetType;

    @Column("thread_id")
    private UUID threadId;

    @Column("post_id")
    private UUID postId;

    @Column("reported_user_id")
    private UUID reportedUserId;

    @Column("report_category")
    private ReportCategory reportCategory;

    @Column("severity")
    private Severity severity;

    @Column("reason")
    private String reason;

    @Column("details")
    private String details;

    @Column("status")
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column("assigned_moderator_id")
    private UUID assignedModeratorId;

    @Column("assigned_at")
    private Instant assignedAt;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Column("reviewed_by")
    private UUID reviewedBy;

    @Column("action_taken")
    private ModerationAction actionTaken;

    @Column("action_taken_details")
    private String actionTakenDetails;

    @Column("resolution_notes")
    private String resolutionNotes;

    @Column("dismissal_reason")
    private DismissalReason dismissalReason;

    @Column("auto_flagged")
    @Builder.Default
    private Boolean autoFlagged = false;

    @Column("related_report_ids")
    private UUID[] relatedReportIds;

    @Column("appeal_id")
    private UUID appealId;

    @CreatedDate
    @Column("reported_at")
    private Instant reportedAt;

    @LastModifiedDate
    @Column("last_modified_at")
    private Instant lastModifiedAt;

    // Helper methods
    public boolean isPending(){
        return status == ReportStatus.PENDING;
    }

    public boolean isUnderReview(){
        return status == ReportStatus.UNDER_REVIEW;
    }

    public boolean isResolvedOrDismissed(){
        return status == ReportStatus.ACTION_TAKEN || status == ReportStatus.DISMISSED;
    }

    public boolean isEscalated() { return  status == ReportStatus.ESCALATED; }

    public boolean isAssigned(){
        return assignedModeratorId != null;
    }

    public boolean isAssignedTo(UUID moderatorId) {
        return assignedModeratorId != null && assignedModeratorId.equals(moderatorId);
    }

    public boolean isAssignedToSomeoneElse(UUID moderatorId) {
        return assignedModeratorId != null && !assignedModeratorId.equals(moderatorId);
    }
}
