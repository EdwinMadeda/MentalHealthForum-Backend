package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.BookmarkSortField;
import com.mentalhealthforum.mentalhealthforum_backend.service.BookmarkService;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
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
@RequestMapping("api/users/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtClaimsExtractor jwtClaimsExtractor;


    public BookmarkController(
            BookmarkService bookmarkService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.bookmarkService = bookmarkService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    @PostMapping
    public Mono<ResponseEntity<StandardSuccessResponse<BookmarkResponse>>> addBookmark(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookmarkRequest request
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return bookmarkService.addBookmark(request, viewerContext)
                .map(bookmark -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("Thread bookmarked successfully", bookmark)));
    }

    @DeleteMapping("/{threadId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> removeBookmark(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return bookmarkService.removeBookmark(threadId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Bookmark removed successfully"))));
    }

    @GetMapping("/{threadId}/check")
    public Mono<ResponseEntity<StandardSuccessResponse<Boolean>>> isBookmarked(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID threadId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return bookmarkService.isBookmarked(threadId, viewerContext)
                .map(isBookmarked -> ResponseEntity.ok(new StandardSuccessResponse<>("Bookmark status retrieved", isBookmarked)));
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<BookmarkResponse>>>> getMyBookmarks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "category_id") UUID categoryId,
            @RequestParam(required = false, name = "creator_id") UUID creatorId,
            @RequestParam(required = false, name = "thread_type") ThreadType threadType,
            @RequestParam(required = false, name = "thread_status") ThreadStatus threadStatus,
            @RequestParam(required = false, name = "has_content_warning")  Boolean hasContentWarning,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(defaultValue = "BOOKMARKED_AT", name = "sort_by") BookmarkSortField sortBy,
            @RequestParam(required = false, name = "sort_direction") String sortDirection
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return bookmarkService.getMyBookmarks(page, size, categoryId, creatorId, threadType, threadStatus, hasContentWarning, search, sortBy, sortDirection, viewerContext)
                .map(bookmarks -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Bookmarks retrieved successfully", bookmarks)));
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<StandardSuccessResponse<Long>>> getBookmarkCountByUserId(
            @AuthenticationPrincipal Jwt jwt
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return bookmarkService.getBookmarkCountByUserId(viewerContext)
                .map(count -> ResponseEntity.ok(new StandardSuccessResponse<>("Bookmark count retrieved", count)));
    }

}
