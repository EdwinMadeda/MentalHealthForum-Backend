package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadStatusDefinitionEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadTypeDefinitionEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ForumThreadService {
    // ==================== USER ACTIONS ====================
    Mono<ThreadResponse> createThread(CreateThreadRequest request, ViewerContext viewerContext);

    Mono<ThreadResponse> getThread(UUID threadId, ViewerContext viewerContext);

    Mono<PaginatedResponse<ThreadResponse>> getAllThreads(
            int page, int size, UUID categoryId,
            UUID creatorId,
            ThreadType threadType,
            ThreadStatus threadStatus,
            Boolean isDeleted,
            Boolean isFeatured,
            Boolean hasContentWarning,
            String search,
            String sortBy,
            String sortDirection,
            ViewerContext viewerContext
    );

    Mono<ThreadResponse> updateOwnThread(UUID threadId, UpdateOwnThreadRequest request, ViewerContext viewerContext);

    Mono<Void> softDeleteOwnThread(UUID threadId, ViewerContext viewerContext);

    Mono<Void> setBestAnswerAsOriginalPoster(UUID threadId, UUID postId, ViewerContext viewerContext);

    // ==================== MODERATOR ACTIONS ====================

    Mono<ThreadResponse> updateThreadStatus(UUID threadId, UpdateThreadStatusRequest request, ViewerContext viewerContext);

    Mono<ThreadResponse> updateThreadType(UUID threadId, UpdateThreadTypeRequest request, ViewerContext viewerContext);

    Mono<ThreadResponse> toggleSticky(UUID threadId, boolean sticky, ViewerContext viewerContext);

    Mono<ThreadResponse> toggleFeatured(UUID threadId, boolean featured, ViewerContext viewerContext);

    Mono<Void> softDeleteThread(UUID threadId, ViewerContext viewerContext);

    Mono<Void> restoreThread(UUID threadId, ViewerContext viewerContext);

    Mono<Void> setBestAnswer(UUID threadId, UUID postId, ViewerContext viewerContext);

    Mono<Void> clearBestAnswer(UUID threadId, ViewerContext viewerContext);

    // ==================== ADMIN ACTIONS ====================

    Mono<Void> permanentlyDeleteThread(UUID threadId, ViewerContext viewerContext);

    // ==================== REFERENCE DATA ====================

    Flux<ThreadTypeDefinitionEntity> getThreadTypes();

    Flux<ThreadStatusDefinitionEntity> getThreadStatuses();
}