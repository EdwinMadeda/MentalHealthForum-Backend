package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.EditReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("thread_edit_history")
public class ThreadEditHistoryEntity {

    @Id
    @Column("id")
    private UUID id;

    @Column("thread_id")
    private UUID threadId;

    @Column("previous_title")
    private String previousTitle;

    @Column("previous_tags")
    private List<String> previousTags;

    @Column("previous_content_warning_type")
    private ContentWarningType previousContentWarningType;

    @Column("previous_content_warning_custom_text")
    private String previousContentWarningCustomText;

    @CreatedDate
    @Column("edited_at")
    private Instant editedAt;

    @Column("edited_by")
    private UUID editedBy;

    @Column("edit_reason_type")
    private EditReason editReasonType;

    @Column("edit_reason_custom_text")
    private String editReasonCustomText;

    @Column("is_moderator_edit")
    private boolean isModeratorEdit;

}
