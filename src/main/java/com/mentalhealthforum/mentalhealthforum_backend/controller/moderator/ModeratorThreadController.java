package com.mentalhealthforum.mentalhealthforum_backend.controller.moderator;

import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.UpdateThreadStatusRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.UpdateThreadTypeRequest;
import com.mentalhealthforum.mentalhealthforum_backend.service.ForumThreadService;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/moderator/forum/threads")
public class ModeratorThreadController {

    private final ForumThreadService forumThreadService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public ModeratorThreadController(
            ForumThreadService forumThreadService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.forumThreadService = forumThreadService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // ==================== MODERATION ACTIONS ====================

    @PatchMapping("/{threadId}/status")
    public Mono<ResponseEntity<StandardSuccessResponse<ThreadResponse>>> updateThreadStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId,
            @Valid @RequestBody UpdateThreadStatusRequest request
            ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.updateThreadStatus(threadId, request, viewerContext)
                .map(thread -> ResponseEntity.ok(new StandardSuccessResponse<>("Thread status updated Successfully",thread)));
    }

    @PatchMapping("/{threadId}/type")
    public Mono<ResponseEntity<StandardSuccessResponse<ThreadResponse>>> updateThreadType(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId,
            @Valid @RequestBody UpdateThreadTypeRequest request
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.updateThreadType(threadId, request, viewerContext)
                .map(thread -> ResponseEntity.ok(new StandardSuccessResponse<>("Thread type updated Successfully",thread)));
    }

    @PatchMapping("/{threadId}/sticky")
    public Mono<ResponseEntity<StandardSuccessResponse<ThreadResponse>>> toggleSticky(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId,
            @RequestParam boolean sticky
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.toggleSticky(threadId, sticky, viewerContext)
                .map(thread -> ResponseEntity.ok(new StandardSuccessResponse<>(sticky?
                        "Thread pinned successfully" :
                        "Thread unpinned successfully",thread)));
    }

    @PatchMapping("/{threadId}/featured")
    public Mono<ResponseEntity<StandardSuccessResponse<ThreadResponse>>> toggleFeatured(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId,
            @RequestParam boolean featured
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.toggleFeatured(threadId, featured, viewerContext)
                .map(thread -> ResponseEntity.ok(new StandardSuccessResponse<>(featured?
                        "Thread featured Successfully" :
                        "Thread un-featured Successfully",thread)));
    }

    @DeleteMapping("/{threadId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> softDeleteThread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.softDeleteThread(threadId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Thread soft deleted Successfully"))));
    }

    @PostMapping("/{threadId}/restore")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> restoreThread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.restoreThread(threadId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Thread restored Successfully"))));
    }

    @PutMapping("/{threadId}/best-answer/{postId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> setBestAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId,
            @PathVariable UUID postId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.setBestAnswer(threadId, postId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Best answer set Successfully"))));
    }

    @DeleteMapping("/{threadId}/best-answer")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> clearBestAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return forumThreadService.clearBestAnswer(threadId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Best answer cleared Successfully"))));
    }
}
