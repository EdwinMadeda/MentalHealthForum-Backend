package com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import org.springframework.data.relational.core.mapping.Column;

public record ReactionCountDto(
        @Column("reaction_type")
        ReactionType reactionType,

        @Column("reaction_count")
        Long reactionCount
) {}

