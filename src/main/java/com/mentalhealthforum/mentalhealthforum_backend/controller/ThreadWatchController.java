package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.WatchThreadResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.WatchThreadSortField;
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
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false, name = "category_id")  UUID categoryId,
            @RequestParam(required = false, name = "creator_id") UUID creatorId,
            @RequestParam(required = false, name = "thread_type")  ThreadType threadType,
            @RequestParam(required = false, name = "thread_status")  ThreadStatus threadStatus,
            @RequestParam(required = false, name = "has_content_warning")  Boolean hasContentWarning,
            @RequestParam(required = false, name = "is_bookmarked")  Boolean isBookmarked,
            @RequestParam(required = false, name = "notification_enabled") Boolean notificationEnabled,
            @RequestParam(required = false, name = "search")  String search,
            @RequestParam(defaultValue = "CREATED_AT", name = "sort_by") WatchThreadSortField sortBy,
            @RequestParam(required = false, name = "sort_direction") String sortDirection
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
