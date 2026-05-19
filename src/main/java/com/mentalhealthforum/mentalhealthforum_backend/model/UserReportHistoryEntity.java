package com.mentalhealthforum.mentalhealthforum_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_report_history")
public class UserReportHistoryEntity {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("total_reports_made")
    @Builder.Default
    private Integer totalReportsMade = 0;

    @Column("reports_upheld")
    @Builder.Default
    private Integer reportsUpheld = 0;

    @Column("reports_dismissed")
    @Builder.Default
    private Integer reportsDismissed = 0;

    // accuracy_rate is GENERATED ALWAYS in DB, not mapped here

    @Column("last_report_at")
    private Instant lastReportAt;

    @Column("is_report_banned")
    @Builder.Default
    private Boolean isReportBanned = false;

    @Column("report_ban_reason")
    private String reportBanReason;

    @Column("report_ban_until")
    private Instant reportBanUntil;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    // Helper methods
    public boolean isBanned(){
        return isReportBanned && (reportBanUntil == null || reportBanUntil.isAfter(Instant.now()));
    }
}
