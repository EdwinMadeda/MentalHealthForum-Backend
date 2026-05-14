package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("reaction_definitions")
public record ReactionDefinitionEntity(
        @Id
        @Column("reaction_type")
        ReactionType reactionType,

        @Column("display_name")
        String displayName,

        @Column("icon_class")
        String iconClass,

        @Column("description")
        String description,

        @Column("reputation_points")
        int reputationPoints,

        @Column("available_to_roles")
        String[] availableToRoles,

        @Column("sort_order")
        int sortOrder,

        @Column("created_at")
        Instant createdAt
) {}
