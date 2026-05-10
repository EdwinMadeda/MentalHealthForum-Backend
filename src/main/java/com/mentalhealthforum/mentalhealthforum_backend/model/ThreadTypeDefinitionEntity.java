package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("thread_type_definitions")
public record ThreadTypeDefinitionEntity(
    @Id
    @Column("thread_type")
    ThreadType threadType,

    @Column("display_name")
    String displayName,

    @Column("description")
    String description,

    @Column("icon_hint")
    String iconHint,

    @Column("example")
    String example
){}
