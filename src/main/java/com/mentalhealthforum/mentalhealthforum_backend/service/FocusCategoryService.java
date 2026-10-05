package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.FocusCategoryResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.FocusCategorySortField;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FocusCategoryService {
    Mono<FocusCategoryResponse> addFocusCategory(UUID categoryId, ViewerContext viewerContext);

    Mono<Void> removeFocusCategory(UUID categoryId, ViewerContext viewerContext);

    Mono<Boolean> isCategoryFocused(UUID categoryId, ViewerContext viewerContext);

    Mono<PaginatedResponse<FocusCategoryResponse>> getFocusCategories(
            int page,
            int size,
            Boolean notificationEnabled,
            String search,
            FocusCategorySortField sortBy,
            String sortDirection,
            ViewerContext viewerContext
    );

    Mono<Long> getFocusCategoriesCount(ViewerContext viewerContext);
}
