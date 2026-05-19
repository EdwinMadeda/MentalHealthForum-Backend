package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportTargetType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("report_history")
public class ReportHistoryEntity {

    @Id
    @Column
    private UUID id;

    @Column("report_id")
    private UUID reportId;

    @Column("action")
    private String action; // e.g., "STATUS_CHANGED", "ASSIGNED", "ACTION_TAKEN"

    @Column("old_value")
    private String oldValue;

    @Column("new_value")
    private String newValue;

    @Column("acted_by")
    private UUID actedBy;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
