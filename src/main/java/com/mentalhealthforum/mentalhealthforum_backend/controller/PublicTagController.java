package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryTagResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.TagSortField;
import com.mentalhealthforum.mentalhealthforum_backend.service.CategoryTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum/tags")
public class PublicTagController {

    private final CategoryTagService categoryTagService;

    public PublicTagController(
            CategoryTagService categoryTagService) {
        this.categoryTagService = categoryTagService;
    }

    // ==================== TAG QUERIES ====================

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<CategoryTagResponse>>>> getAllTags(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(defaultValue = "NAME", name = "sort_by")  TagSortField sortBy,
            @RequestParam(required = false, name = "sort_direction")  String sortDirection
    ) {
        return categoryTagService.getAllTags(page, size, search, sortBy, sortDirection)
                .map(response ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Tags retrieved successfully", response)));
    }

    @GetMapping("{tagId}")
    public Mono<ResponseEntity<StandardSuccessResponse<CategoryTagResponse>>> getTagById(
            @PathVariable UUID tagId
       ) {
        return categoryTagService.getTagById(tagId)
                .map(tag -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Tag retrieved successfully", tag)
                ));
    }

    @GetMapping("/by-slug/{slug}")
    public Mono<ResponseEntity<StandardSuccessResponse<CategoryTagResponse>>> getTagBySlug(
            @PathVariable String slug
    ) {
        return categoryTagService.getTagBySlug(slug)
                .map(tag -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Tag retrieved successfully", tag)
                ));
    }


    @GetMapping("/categories/{categoryId}")
    public Mono<ResponseEntity<StandardSuccessResponse<List<CategoryTagResponse>>>> getTagsForCategory(
            @PathVariable UUID categoryId) {
        return categoryTagService.getEnrichedTagsForCategory(categoryId)
                .collectList()
                .map(tags ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Tags retrieved successfully", tags)));
    }



}
