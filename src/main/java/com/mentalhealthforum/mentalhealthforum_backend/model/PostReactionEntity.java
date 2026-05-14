package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
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
@Table("post_reactions")
public class PostReactionEntity {

    @Id
    @Column("id")
    private UUID id;

    @Column("post_id")
    private UUID postId;

    @Column("user_id")
    private UUID userId;

    @Column("reaction_type")
    private ReactionType reactionType;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
