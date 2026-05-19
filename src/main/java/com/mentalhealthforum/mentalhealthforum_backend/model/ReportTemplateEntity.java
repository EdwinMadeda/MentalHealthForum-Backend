package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
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
@Table("report_templates")
public class ReportTemplateEntity {

    @Id
    @Column
    private UUID id;

    @Column("report_category")
    private ReportCategory reportCategory;

    @Column("template_text")
    private String templateText;

    @Column("requires_details")
    @Builder.Default
    private Boolean requiresDetails = false;

    @Column("auto_severity")
    private Severity autoSeverity;

    @Column("display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
