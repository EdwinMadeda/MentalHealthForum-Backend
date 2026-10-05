package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.CreatePostRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.PostResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.UpdatePostRequest;
import com.mentalhealthforum.mentalhealthforum_backend.enums.PostType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.PostSortField;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.PostService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/forum/posts")
public class PublicPostController {

    private final PostService postService;
    private final JwtClaimsExtractor jwtClaimsExtractor;


    public PublicPostController(PostService postService, JwtClaimsExtractor jwtClaimsExtractor) {
        this.postService = postService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // ==================== CREATE / READ ====================

    @PostMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PostResponse>>> createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePostRequest request
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.createPost(request, viewerContext)
                .map(post -> ResponseEntity.status(HttpStatus.CREATED)
                .body(new StandardSuccessResponse<>("Post created successfully", post)));
    }

    @GetMapping("/{postId}")
    public Mono<ResponseEntity<StandardSuccessResponse<PostResponse>>> getPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.getPost(postId, viewerContext)
                .map(post ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Post retrieved successfully", post)));

    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<PostResponse>>>> getAllPosts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "thread_id")  UUID threadId,
            @RequestParam(required = false, name = "author_id")  UUID authorId,
            @RequestParam(required = false, name = "parent_post_id")  UUID parentPostId,
            @RequestParam(required = false, name = "post_type")  PostType postType,
            @RequestParam(required = false, name = "has_content_warning") Boolean hasContentWarning,
            @RequestParam(defaultValue = "false", name = "is_deleted") boolean isDeleted,
            @RequestParam(required = false, name = "search")  String search,
            @RequestParam(defaultValue = "CREATED_AT", name = "sort_by") PostSortField sortBy,
            @RequestParam(required = false, name = "sort_direction")  String sortDirection
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.getAllPosts( page, size, threadId, authorId, parentPostId, postType, hasContentWarning, isDeleted, search, sortBy, sortDirection, viewerContext)
                .map(paginatedPosts ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Posts retrieved successfully", paginatedPosts)));
    }

    @PutMapping("/{postId}")
    public Mono<ResponseEntity<StandardSuccessResponse<PostResponse>>> updateOwnPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @Valid@RequestBody UpdatePostRequest request
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.updateOwnPost(postId,request, viewerContext)
                .map(post ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Post updated successfully", post)));
    }

    @DeleteMapping("/{postId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> deleteOwnPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return postService.softDeleteOwnPost(postId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Post soft deleted successfully"))));
    }
}
