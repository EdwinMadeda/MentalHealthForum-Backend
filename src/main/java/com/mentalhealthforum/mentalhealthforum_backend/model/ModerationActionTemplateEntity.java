package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
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
@Table("moderation_action_templates")
public class ModerationActionTemplateEntity {

    @Id
    private UUID id;

    @Column("action_type")
    private ModerationAction actionType;

    @Column("default_message")
    private String defaultMessage;

    @Column("description")
    private String description;

    @Column("example_message")
    private String exampleMessage;

    @Column("display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column("created_at")
    private Instant CreatedAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

}
