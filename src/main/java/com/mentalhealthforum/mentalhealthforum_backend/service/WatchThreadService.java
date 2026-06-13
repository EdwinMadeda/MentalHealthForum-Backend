package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.WatchThreadResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WatchThreadService {
    Mono<WatchThreadResponse> watchThread(UUID threadId, ViewerContext viewerContext);

    Mono<Void> unwatchThread(UUID threadId, ViewerContext viewerContext);

    Mono<Boolean> isWatchingThread(UUID threadId, ViewerContext viewerContext);

    Mono<PaginatedResponse<WatchThreadResponse>> getWatchThreads(
            int page,
            int size,
            UUID categoryId,
            UUID creatorId,
            ThreadType threadType,
            ThreadStatus threadStatus,
            Boolean hasContentWarning,
            Boolean isBookmarked,
            Boolean notificationEnabled,
            String search,
            String sortBy,
            String sortDirection,
            ViewerContext viewerContext
    );

    Mono<Long> getWatchThreadCount(ViewerContext viewerContext);
}
