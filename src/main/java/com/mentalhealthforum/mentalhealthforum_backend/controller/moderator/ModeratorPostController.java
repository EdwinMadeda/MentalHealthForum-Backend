package com.mentalhealthforum.mentalhealthforum_backend.controller.moderator;

import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.AddContentWarningRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.PostResponse;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/moderator/forum/posts")
public class ModeratorPostController {

    private final PostService postService;
    private final JwtClaimsExtractor jwtClaimsExtractor;


    public ModeratorPostController(PostService postService, JwtClaimsExtractor jwtClaimsExtractor) {
        this.postService = postService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // ==================== MODERATION ACTIONS ====================

    @PatchMapping("/{postId}/content-warning")
    public Mono<ResponseEntity<StandardSuccessResponse<PostResponse>>> addContentWarning(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @Valid @RequestBody AddContentWarningRequest request
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.addContentWarning(postId, request, viewerContext)
                .map(post -> ResponseEntity.ok(new StandardSuccessResponse<>("Content warning added successfully", post)));
    }



    @DeleteMapping("/{postId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> softDeleteAnyPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.softDeleteAnyPost(postId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Post soft deleted successfully"))));
    }

    @PostMapping("/{postId}/restore")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> restorePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.restorePost(postId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Post restored successfully"))));
    }

}
