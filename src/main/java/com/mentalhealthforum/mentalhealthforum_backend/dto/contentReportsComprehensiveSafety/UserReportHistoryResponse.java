package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
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
public class UserReportHistoryResponse {

    private UUID userId;
    private Integer totalReportsMade;
    private Integer reportsUpheld;
    private Integer reportsDismissed;
    private Double accuracyRate;
    private Instant lastReportAt;

    // TODO: Future - Report ban functionality
    // When a user abuses the report system, admins can:
    // 1. Ban user from submitting reports for a period
    // 2. Permanent ban for repeat offenders
    // 3. Ban reason stored for transparency
    // 4. User sees ban message when trying to report

    private boolean isReportBanned;
    private Instant reportBanUntil;
    private String reportBanReason;
    private Instant createdAt;
    
}
