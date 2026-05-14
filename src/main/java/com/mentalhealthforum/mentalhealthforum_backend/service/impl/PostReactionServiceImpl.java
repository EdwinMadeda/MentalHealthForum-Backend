package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.model.PostReactionEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.ReactionDefinitionEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.PostReactionRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.PostRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.ReactionDefinitionRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.PostReactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class PostReactionServiceImpl implements PostReactionService {

    private static final Logger log = LoggerFactory.getLogger(PostReactionServiceImpl.class);

    private final TransactionalOperator transactionalOperator;
    private final PostReactionRepository postReactionRepository;
    private final PostRepository postRepository;
    private final ReactionDefinitionRepository reactionDefinitionRepository;

    public PostReactionServiceImpl(
            TransactionalOperator transactionalOperator,
            PostReactionRepository postReactionRepository,
            PostRepository postRepository,
            ReactionDefinitionRepository reactionDefinitionRepository) {
        this.transactionalOperator = transactionalOperator;
        this.postReactionRepository = postReactionRepository;
        this.postRepository = postRepository;
        this.reactionDefinitionRepository = reactionDefinitionRepository;
    }


    // ==================== REACTION OPERATIONS ====================

    @Override
    public Mono<PostReactionSummaryResponse> addPostReaction(UUID postId, AddReactionRequest request, ViewerContext viewerContext) {

        UUID userId = UUID.fromString(viewerContext.getUserId());
        ReactionType newReaction = request.reactionType();

        // First, verify that post exists and is not deleted
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .switchIfEmpty(Mono.error(new ApiException("Post not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(post -> {
                    // Don't allow reacting to your own post
                    if(post.getAuthorId().equals(userId)){
                        return Mono.error(new ApiException("You cannot react to your own posts", ErrorCode.VALIDATION_FAILED));
                    }
                    return Mono.just(post);
                })
                // check if user already has a reaction (any type) on this post
                .flatMap(post -> postReactionRepository.findByPostIdAndUserId(postId, userId))
                .flatMap(existingReaction -> {
                    if(existingReaction.getReactionType() == newReaction){
                        // Same reaction - idempotent, return current state
                        return getPostReactions(postId, viewerContext);
                    }
                    else {
                        // Different reaction - delete old, add new
                        return postReactionRepository.deleteByPostIdAndUserId(postId, userId)
                                .then(createReaction(postId, userId, newReaction))
                                .then(getPostReactions(postId, viewerContext));
                    }

                })
                .switchIfEmpty(Mono.defer(()->
                        // No existing reaction - add new
                        createReaction(postId, userId, newReaction)
                                .then(getPostReactions(postId, viewerContext))
                        ))
                .as(transactionalOperator::transactional);
        
    }

    @Override
    public Mono<PostReactionSummaryResponse> removePostReaction(UUID postId, ViewerContext viewerContext) {
        UUID userId = UUID.fromString(viewerContext.getUserId());

        // First, verify that post exists and is not deleted
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .switchIfEmpty(Mono.error(new ApiException("Post not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(post -> postReactionRepository.findByPostIdAndUserId(postId, userId))
                .flatMap(existingReaction -> postReactionRepository.deleteByPostIdAndUserId(postId, userId)
                        .then(getPostReactions(postId, viewerContext)))
                .switchIfEmpty(Mono.defer(()->
                    // No reaction to remove - return current state
                    getPostReactions(postId, viewerContext)
        ))
        .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<PostReactionSummaryResponse> getPostReactions(UUID postId, ViewerContext viewerContext) {
        UUID userId = UUID.fromString(viewerContext.getUserId());

        // Validate the post exists and is not deleted
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .switchIfEmpty(Mono.error(new ApiException("Post not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(post -> Mono.zip(
                        // Get reaction definitions (reference data)
                        reactionDefinitionRepository.findAllByOrderBySortOrderAsc()
                                .collectList()
                                .doOnNext(definitions -> log.debug("Definitions count: {}", definitions.size())),

                        // Get reaction counts grouped by type. Returns: Map<ReactionType, Long>
                        postReactionRepository.countReactionsByType(postId)
                                .collectMap(
                                        ReactionCountDto::reactionType,
                                        ReactionCountDto::reactionCount)
                                .defaultIfEmpty(Collections.emptyMap())
                                        .doOnNext(map -> log.debug("Count map: {}", map)),

                        // Get user's reaction on this post
                        postReactionRepository.findByPostIdAndUserId(postId, userId)
                                .map(postReaction -> Optional.of(postReaction.getReactionType()))
                                .defaultIfEmpty(Optional.empty())
                                .doOnNext(reaction -> log.debug("User reaction: {}", reaction))
                ))
                .map(tuple -> {
                    Map<ReactionType, Long> counts = tuple.getT2();
                    Optional<ReactionType> userReactionOpt = tuple.getT3();
                    List<ReactionDefinitionEntity> definitions = tuple.getT1();

                    // Build reaction summaries for each definition
                    List<ReactionSummaryResponse> reactions = definitions.stream()
                            .map(definition -> ReactionSummaryResponse.builder()
                                    .reactionType(definition.reactionType())
                                    .displayName(definition.displayName())
                                    .iconClass(definition.iconClass())
                                    // Get count from map, default to 0 if not present
                                    .count(counts.getOrDefault(definition.reactionType(), 0L).intValue())
                                    // Does current user have this reaction?
                                    .userReacted(userReactionOpt.isPresent() && userReactionOpt.get() == definition.reactionType())
                                    .build())
                            .toList();

                    // Calculate total reactions (sum of all counts)
                    int totalReactionCount = reactions.stream().mapToInt(ReactionSummaryResponse::getCount).sum();

                    //  Build final response
                    return PostReactionSummaryResponse.builder()
                            .totalReactionCount(totalReactionCount)
                            .reactions(reactions)
                            .build();
                });
    }

    @Override
    public Flux<ReactionDefinitionResponse> getReactionDefinitions() {
        return reactionDefinitionRepository.findAllByOrderBySortOrderAsc()
                .map(definition -> ReactionDefinitionResponse.builder()
                        .reactionType(definition.reactionType())
                        .displayName(definition.displayName())
                        .iconClass(definition.iconClass())
                        .description(definition.description())
                        .sortOrder(definition.sortOrder())
                        .build());
    }

    // ==================== PRIVATE HELPERS ====================
    private Mono<Void> createReaction(UUID postId, UUID userId, ReactionType reactionType) {
        PostReactionEntity postReaction = PostReactionEntity.builder()
                .postId(postId)
                .userId(userId)
                .reactionType(reactionType)
                .build();
        return postReactionRepository.save(postReaction).then();
    }

}
