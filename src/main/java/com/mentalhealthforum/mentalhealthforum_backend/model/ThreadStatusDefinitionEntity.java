package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("thread_status_definitions")
public record ThreadStatusDefinitionEntity(
        @Id
        @Column("thread_status")
        ThreadStatus threadStatus,

        @Column("display_name")
        String displayName,

        @Column("description")
        String description,

        @Column("user_visible")
        Boolean userVisible
) { }
