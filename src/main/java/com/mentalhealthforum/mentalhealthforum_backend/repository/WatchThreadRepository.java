package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.model.WatchThreadEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface WatchThreadRepository extends R2dbcRepository<WatchThreadEntity, UUID> {

    Mono<Boolean> existsByUserIdAndThreadId(UUID userId, UUID threadId);

    Mono<Void> deleteByUserIdAndThreadId(UUID userId, UUID threadId);

    Mono<Long> countByUserId(UUID userId);

    @Query("""
        SELECT wt.* FROM watch_threads wt
        INNER JOIN forum_threads t ON wt.thread_id = t.id
        WHERE wt.user_id = :userId
            AND t.is_deleted = false
            AND (:search IS NULL OR
                 LOWER(t.title) LIKE '%' || LOWER(:search) || '%')
            AND (:notificationEnabled IS NULL OR wt.notification_enabled = :notificationEnabled)
            AND (:categoryId IS NULL OR t.category_id = :categoryId)
            AND (:creatorId IS NULL OR t.creator_id = :creatorId)
            AND (:threadType IS NULL OR t.thread_type = :threadType::thread_type_enum)
            AND (:threadStatus IS NULL OR t.thread_status = :threadStatus::thread_status_enum)
            AND (:hasContentWarning IS NULL OR
                (CASE WHEN :hasContentWarning = true
                 THEN t.content_warning_type != 'NONE'
                 ELSE t.content_warning_type = 'NONE' END))
            AND (:isBookmarked IS NULL OR
                (:isBookmarked = true AND EXISTS (
                    SELECT 1 FROM thread_bookmarks b
                    WHERE b.thread_id = t.id AND b.user_id = :userId
                    )) OR
                (:isBookmarked = false AND NOT EXISTS (
                    SELECT 1 FROM thread_bookmarks b
                    WHERE b.thread_id = t.id AND b.user_id = :userId
                    ))
                )
 
        ORDER BY
            CASE :sortDirection
                WHEN 'DESC' THEN
                    CASE :sortBy
                        WHEN 'created_at' THEN wt.created_at::text
                        WHEN 'last_activity_at' THEN t.last_activity_at::text
                        WHEN 'thread_title' THEN t.title
                        WHEN 'post_count' THEN LPAD(t.post_count::text, 10, '0')
                        WHEN 'view_count' THEN LPAD(t.view_count::text, 10, '0')
                        ELSE wt.created_at::text
                    END
                ELSE NULL
            END DESC NULLS LAST,
    
    
            CASE :sortDirection
                WHEN 'ASC' THEN
                    CASE :sortBy
                        WHEN 'created_at' THEN wt.created_at::text
                        WHEN 'last_activity_at' THEN t.last_activity_at::text
                        WHEN 'thread_title' THEN t.title
                        WHEN 'post_count' THEN LPAD(t.post_count::text, 10, '0')
                        WHEN 'view_count' THEN LPAD(t.view_count::text, 10, '0')
                        ELSE wt.created_at::text
                    END
                ELSE NULL
            END ASC NULLS FIRST
        LIMIT :limit OFFSET :offset
    """)
    Flux<WatchThreadEntity> findPaginatedByUserId(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("creatorId") UUID creatorId,
            @Param("threadType") String threadType,
            @Param("threadStatus") String threadStatus,
            @Param("hasContentWarning") Boolean hasContentWarning,
            @Param("isBookmarked") Boolean isBookmarked,
            @Param("notificationEnabled") Boolean notificationEnabled,
            @Param("search") String search,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT COUNT(*) FROM watch_threads wt
        INNER JOIN forum_threads t ON wt.thread_id = t.id
        WHERE wt.user_id = :userId
            AND t.is_deleted = false
            AND (:search IS NULL OR
                 LOWER(t.title) LIKE '%' || LOWER(:search) || '%')
            AND (:notificationEnabled IS NULL OR wt.notification_enabled = :notificationEnabled)
            AND (:categoryId IS NULL OR t.category_id = :categoryId)
            AND (:creatorId IS NULL OR t.creator_id = :creatorId)
            AND (:threadType IS NULL OR t.thread_type = :threadType::thread_type_enum)
            AND (:threadStatus IS NULL OR t.thread_status = :threadStatus::thread_status_enum)
            AND (:hasContentWarning IS NULL OR
                (CASE WHEN :hasContentWarning = true
                 THEN t.content_warning_type != 'NONE'
                 ELSE t.content_warning_type = 'NONE' END))
            AND (:isBookmarked IS NULL OR
                (:isBookmarked = true AND EXISTS (
                    SELECT 1 FROM thread_bookmarks b
                    WHERE b.thread_id = t.id AND b.user_id = :userId
                    )) OR
                (:isBookmarked = false AND NOT EXISTS (
                    SELECT 1 FROM thread_bookmarks b
                    WHERE b.thread_id = t.id AND b.user_id = :userId
                    ))
                )
    """)
    Mono<Long> countByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("creatorId") UUID creatorId,
            @Param("threadType") String threadType,
            @Param("threadStatus") String threadStatus,
            @Param("hasContentWarning") Boolean hasContentWarning,
            @Param("isBookmarked") Boolean isBookmarked,
            @Param("notificationEnabled") Boolean notificationEnabled,
            @Param("search") String search
    );

}
