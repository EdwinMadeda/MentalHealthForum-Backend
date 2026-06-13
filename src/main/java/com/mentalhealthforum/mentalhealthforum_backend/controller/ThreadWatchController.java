package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.WatchThreadResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import com.mentalhealthforum.mentalhealthforum_backend.service.WatchThreadService;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/watch-threads")
public class ThreadWatchController {

    private final WatchThreadService watchThreadService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public ThreadWatchController(
            WatchThreadService watchThreadService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.watchThreadService = watchThreadService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    @PostMapping("/{threadId}")
    public Mono<ResponseEntity<StandardSuccessResponse<WatchThreadResponse>>> watchThread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return watchThreadService.watchThread(threadId, viewerContext)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("Thread added to watch list", response)));
    }

    @DeleteMapping("/{threadId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> unwatchThread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return watchThreadService.unwatchThread(threadId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Thread removed from watch list"))));
    }


    @GetMapping("/{threadId}/watched")
    public Mono<ResponseEntity<StandardSuccessResponse<Boolean>>> isWatchingThread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return watchThreadService.isWatchingThread(threadId, viewerContext)
                .map(watched -> ResponseEntity.ok(new StandardSuccessResponse<>("Watch status retrieved successfully", watched)));
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<WatchThreadResponse>>>> getWatchThreads(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Number of items per page") int size,
            @RequestParam(required = false, name = "category_id") @Parameter(name = "category_id", description = "Filter by category ID") UUID categoryId,
            @RequestParam(required = false, name = "creator_id") @Parameter(name = "creator_id", description = "Filter by creator user ID") UUID creatorId,
            @RequestParam(required = false, name = "thread_type") @Parameter(name = "thread_type", description = "Filter by thread type: DISCUSSION, QUESTION, CRISIS_SUPPORT, PEER_REVIEW, POLL") ThreadType threadType,
            @RequestParam(required = false, name = "thread_status") @Parameter(name = "thread_status", description = "Filter by thread status: OPEN, RESOLVED, CLOSED, ARCHIVED") ThreadStatus threadStatus,
            @RequestParam(required = false, name = "has_content_warning") @Parameter(name = "has_content_warning", description = "Filter threads with content warnings") Boolean hasContentWarning,
            @RequestParam(required = false) @Parameter(description = "Filter by bookmark status: true (bookmarked), false (not bookmarked)") Boolean isBookmarked,
            @RequestParam(required = false) @Parameter(description = "Filter by notification enabled status") Boolean notificationEnabled,
            @RequestParam(defaultValue = "") @Parameter(description = "Search by thread title or content") String search,
            @RequestParam(defaultValue = "created_at") @Parameter(description = "Sort field: created_at, thread_title") String sortBy,
            @RequestParam(required = false) @Parameter(description = "Sort direction: asc or desc") String sortDirection
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return watchThreadService.getWatchThreads(page, size, categoryId, creatorId, threadType, threadStatus, hasContentWarning, isBookmarked, notificationEnabled, search, sortBy, sortDirection, viewerContext)
                .map(threads ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Watch threads retrieved successfully", threads)));
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<StandardSuccessResponse<Long>>> getWatchThreadsCount(
            @AuthenticationPrincipal Jwt jwt
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return watchThreadService.getWatchThreadCount(viewerContext)
                .map(count -> ResponseEntity.ok(new StandardSuccessResponse<>("Watch threads retrieved successfully", count)));
    }

}
