package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.ReactionCountDto;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import com.mentalhealthforum.mentalhealthforum_backend.model.PostReactionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PostReactionRepository extends R2dbcRepository<PostReactionEntity, UUID> {

    Flux<PostReactionEntity> findByPostId(UUID postId);

    Mono<PostReactionEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    @Query("""
    SELECT reaction_type, COUNT(*) as reaction_count
    FROM post_reactions
    WHERE post_id = :postId
    GROUP BY reaction_type
""")
    Flux<ReactionCountDto> countReactionsByType(@Param("postId") UUID postId);

    Mono<Boolean> existsByPostIdAndUserIdAndReactionType(UUID postId, UUID userId, ReactionType reactionType);

    Mono<Void> deleteByPostIdAndUserId(UUID postId, UUID userId);

    Mono<Void> deleteByPostIdAndUserIdAndReactionType(UUID postId, UUID userId, ReactionType reactionType);

}
