package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.AddReactionRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.PostReactionSummaryResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport.ReactionDefinitionResponse;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.PostReactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/forum/posts")
public class PublicReactionController {

    private final PostReactionService postReactionService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public PublicReactionController(
            PostReactionService postReactionService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.postReactionService = postReactionService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // ==================== POST REACTIONS ====================

    /**
     * Get reaction summary for a post.
     * Returns aggregated counts only - no individual user data.
     */
    @GetMapping("/{postId}/reactions")
    public Mono<ResponseEntity<StandardSuccessResponse<PostReactionSummaryResponse>>> getPostReactions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postReactionService.getPostReactions(postId, viewerContext)
                .map(reactionSummary -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Reactions retrieved successfully", reactionSummary)
                ));
    }

    /**
     * Add a reaction to a post.
     * If user already has a different reaction, it will be replaced.
     * If user already has the same reaction, no change (idempotent).
     */
    @PostMapping("/{postId}/reactions")
    public Mono<ResponseEntity<StandardSuccessResponse<PostReactionSummaryResponse>>> addPostReactions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @Valid @RequestBody AddReactionRequest request
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postReactionService.addPostReaction(postId, request, viewerContext)
                .map(reactionSummary -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Reactions added successfully", reactionSummary)
                ));
    }

    /**
     * Remove user's reaction from a post.
     */
    @DeleteMapping("/{postId}/reactions")
    public Mono<ResponseEntity<StandardSuccessResponse<PostReactionSummaryResponse>>> removePostReaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postReactionService.removePostReaction(postId,viewerContext)
                .map(reactionSummary -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Reaction removed successfully", reactionSummary)
                ));
    }

    // ==================== REFERENCE DATA ====================
    /**
     * Get all available reaction definitions.
     * Used by frontend to build reaction UI (buttons, icons, tooltips).
     * No authentication required - reference data is public.
     */
    @GetMapping("/reaction-definitions")
    public Mono<ResponseEntity<StandardSuccessResponse<List<ReactionDefinitionResponse>>>> getReactionDefinitions(){

        return postReactionService.getReactionDefinitions()
                .collectList()
                .map(definitions -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Reaction definitions retrieved successfully", definitions)
                ));
    }

}
