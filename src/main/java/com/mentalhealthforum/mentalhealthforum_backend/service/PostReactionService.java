package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.AddReactionRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.PostReactionSummaryResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.ReactionDefinitionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PostReactionService {

    // ==================== REACTION OPERATIONS ====================

    /**
     * Add a reaction to a post.
     * If user already has a reaction of the same type, this is idempotent (no change).
     * If user has a different reaction type, it will be replaced.
     *
     * @param postId ID of the post to react to
     * @param request Contains the reaction type
     * @param viewerContext Current user's context
     * @return Summary of all reactions after the operation
     */
    Mono<PostReactionSummaryResponse> addPostReaction(UUID postId, AddReactionRequest request, ViewerContext viewerContext);

    /**
     * Remove a user's reaction from a post.
     *
     * @param postId ID of the post to remove reaction from
     * @param viewerContext Current user's context
     * @return Summary of all reactions after removal
     */
    Mono<PostReactionSummaryResponse> removePostReaction(UUID postId, ViewerContext viewerContext);

    /**
     * Get all reactions for a post.
     *
     * @param postId        ID of the post
     * @param viewerContext Current user's context (to determine which reactions they made)
     * @return Summary of all reactions on the post
     */
    Mono<PostReactionSummaryResponse> getPostReactions(UUID postId, ViewerContext viewerContext);

    // ==================== REFERENCE DATA ====================
    /**
     * Get all reaction definitions (for UI dropdowns, buttons, etc.)
     *
     * @return List of all available reaction types with their metadata
     */
    Flux<ReactionDefinitionResponse> getReactionDefinitions();



}
