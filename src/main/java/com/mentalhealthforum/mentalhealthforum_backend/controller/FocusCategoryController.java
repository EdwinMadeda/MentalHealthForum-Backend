package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.FocusCategoryResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.FocusCategorySortField;
import com.mentalhealthforum.mentalhealthforum_backend.service.FocusCategoryService;
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
@RequestMapping("/api/users/focus-categories")
public class FocusCategoryController {

    private final FocusCategoryService focusCategoryService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public FocusCategoryController(
            FocusCategoryService focusCategoryService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.focusCategoryService = focusCategoryService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    @PostMapping("/{categoryId}")
    public Mono<ResponseEntity<StandardSuccessResponse<FocusCategoryResponse>>> addFocusCategory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID categoryId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return focusCategoryService.addFocusCategory(categoryId, viewerContext)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("Category added to focus list", response)));
    }

    @DeleteMapping("/{categoryId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> removeFocusCategory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID categoryId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return focusCategoryService.removeFocusCategory(categoryId, viewerContext)
                .then(Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Category removed from focus list"))));
    }


    @GetMapping("/{categoryId}/focused")
    public Mono<ResponseEntity<StandardSuccessResponse<Boolean>>> isCategoryFocused(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID categoryId
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return focusCategoryService.isCategoryFocused(categoryId, viewerContext)
                .map(focused -> ResponseEntity.ok(new StandardSuccessResponse<>("Focus status retrieved successfully", focused)));
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<FocusCategoryResponse>>>> getFocusCategories(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "notification_enabled")  Boolean notificationEnabled,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(defaultValue = "CREATED_AT", name = "sort_by") FocusCategorySortField sortBy,
            @RequestParam(required = false, name = "sort_direction")  String sortDirection
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return focusCategoryService.getFocusCategories(page, size,  notificationEnabled,search, sortBy,sortDirection, viewerContext)
                .map(categories ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Focus categories retrieved successfully", categories)));
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<StandardSuccessResponse<Long>>> getFocusCategoriesCount(
            @AuthenticationPrincipal Jwt jwt
    ){
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return focusCategoryService.getFocusCategoriesCount(viewerContext)
                .map(count -> ResponseEntity.ok(new StandardSuccessResponse<>("Focus categories retrieved successfully", count)));
    }

}
