package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkCountRecord;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkStatusRecord;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkedThreadRecord;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadBookmarkEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreadBookmarkRepository extends R2dbcRepository<ThreadBookmarkEntity, UUID> {

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM thread_bookmarks b
            INNER JOIN forum_threads t ON b.thread_id = t.id
            INNER JOIN forum_categories c ON t.category_id = c.id
            WHERE b.user_id = :userId
                AND b.thread_id = :threadId
                AND t.is_deleted = false
                AND category_is_visible(c.id, :userId, :isAdmin, :isModeratorOrAdmin, :isVerified)
        )
    """)
    Mono<Boolean> existsVisibleByUserIdAndThreadId(
            @Param("userId") UUID userId,
            @Param("threadId") UUID threadId,
            @Param("isAdmin") boolean isAdmin,
            @Param("isModeratorOrAdmin") boolean isModeratorOrAdmin,
            @Param("isVerified") boolean isVerified
    );

    Mono<Boolean> existsByUserIdAndThreadId(UUID userId, UUID threadId);

    Mono<Void> deleteByUserIdAndThreadId(UUID userId, UUID threadId);

    /**
     * Batch fetch bookmark status for threads
     */
    @Query("""
        SELECT b.thread_id as thread_id, true as is_bookmarked
        FROM thread_bookmarks b
        WHERE b.thread_id IN (:threadIds) AND b.user_id = :userId
    """)
    Flux<BookmarkStatusRecord> findBookmarkStatusForThreads(
        @Param("userId") UUID userId,
        @Param("threadIds") List<UUID> threadIds
    );

    /**
     * Batch fetch bookmark status for threads
     */
    @Query("""
        SELECT thread_id, COUNT(*) as count
        FROM thread_bookmarks
        WHERE thread_id IN (:threadIds)
        GROUP BY thread_id
    """)
    Flux<BookmarkCountRecord> findBookmarkCountsForThreads(
            @Param("threadIds") List<UUID> threadIds
    );


    @Query("""
        SELECT b.id as bookmark_id,
               t.id as thread_id,
               t.title,
               t.category_id,
               t.creator_id,
               t.post_count,
               t.view_count,
               t.last_activity_at,
               t.thread_status,
               t.thread_type,
               t.content_warning_type,
               b.created_at as bookmarked_at,
               b.notes as bookmark_notes
        FROM thread_bookmarks b
        INNER JOIN forum_threads t ON b.thread_id = t.id
        WHERE b.id = :bookmarkId AND b.user_id = :userId
        """)
    Mono<BookmarkedThreadRecord> findBookmarkById(
        @Param("bookmarkId") UUID bookmarkId,
        @Param("userId") UUID userId
    );

    @Query("""
        SELECT b.id as bookmark_id,
               t.id as thread_id,
               t.title,
               t.category_id,
               t.creator_id,
               t.post_count,
               t.view_count,
               t.last_activity_at,
               t.thread_status,
               t.thread_type,
               t.content_warning_type,
               b.created_at as bookmarked_at,
               b.notes as bookmark_notes
        FROM forum_threads t
        INNER JOIN thread_bookmarks b ON t.id = b.thread_id
        INNER JOIN forum_categories c ON t.category_id = c.id
        WHERE b.user_id = :viewerId
        AND t.is_deleted = false
    
        AND (:search IS NULL
                -- FTS on Title (stemming for content)
                OR to_tsvector('public.english_unaccent', coalesce(t.title, ''))
                    @@ websearch_to_tsquery('public.english_unaccent', :search)
    
                -- FTS on Notes (stemming for notes too)
                OR to_tsvector('public.english_unaccent', coalesce(b.notes, ''))
                    @@ websearch_to_tsquery('public.english_unaccent', :search)
    
                -- Trigram fallback on Title (typos/partials)
                OR public.unaccent_immutable(t.title) % public.unaccent_immutable(:search)
    
                -- Triagram fallback on Notes
                OR public.unaccent_immutable(b.notes) % public.unaccent_immutable(:search)
        )
    
        -- Category visibility
        AND category_is_visible(c.id, :viewerId, :isAdmin, :isModeratorOrAdmin, :isVerified)
   
        AND (:categoryId IS NULL OR t.category_id = :categoryId)
        AND (:creatorId IS NULL OR t.creator_id = :creatorId)
        AND (:threadType IS NULL OR t.thread_type = :threadType::thread_type_enum)
        AND (:threadStatus IS NULL OR t.thread_status = :threadStatus::thread_status_enum)
        AND (:hasContentWarning IS NULL OR
                (CASE WHEN :hasContentWarning = true
                    THEN t.content_warning_type != 'NONE'
                    ELSE t.content_warning_type = 'NONE'
                END)
            )
    
        ORDER BY
            CASE :sortDirection
                WHEN 'DESC' THEN
                    CASE :sortBy
                        WHEN 'title' THEN t.title
                        WHEN 'bookmarked_at' THEN b.created_at::text
                        WHEN 'last_activity_at' THEN t.last_activity_at::text
                        WHEN 'post_count' THEN LPAD(t.post_count::text, 10, '0')
                        ELSE b.created_at::text
                    END
                ELSE NULL
            END DESC NULLS LAST,
    
            CASE :sortDirection
                WHEN 'ASC' THEN
                    CASE :sortBy
                        WHEN 'title' THEN t.title
                        WHEN 'bookmarked_at' THEN b.created_at::text
                        WHEN 'last_activity_at' THEN t.last_activity_at::text
                        WHEN 'post_count' THEN LPAD(t.post_count::text, 10, '0')
                        ELSE b.created_at::text
                    END
                ELSE NULL
            END ASC NULLS FIRST
        LIMIT :limit OFFSET :offset
    """)
    Flux<BookmarkedThreadRecord> findBookmarkedThreadsPaginated(
            @Param("viewerId") UUID viewerId,
            @Param("isAdmin") boolean isAdmin,
            @Param("isModeratorOrAdmin") boolean isModeratorOrAdmin,
            @Param("isVerified") boolean isVerified,
            @Param("categoryId") UUID categoryId,
            @Param("creatorId") UUID creatorId,
            @Param("threadType") String threadType,
            @Param("threadStatus") String threadStatus,
            @Param("hasContentWarning") Boolean hasContentWarning,
            @Param("search") String search,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT COUNT(*) FROM thread_bookmarks b
        INNER JOIN forum_threads t ON b.thread_id = t.id
        INNER JOIN forum_categories c ON t.category_id = c.id
        WHERE b.user_id = :viewerId
        AND t.is_deleted = false

        AND (:search IS NULL
    
            OR to_tsvector('public.english_unaccent', coalesce(t.title, ''))
                @@ websearch_to_tsquery('public.english_unaccent', :search)

            OR to_tsvector('public.english_unaccent', coalesce(b.notes, ''))
                @@ websearch_to_tsquery('public.english_unaccent', :search)
    
            OR public.unaccent_immutable(t.title) % public.unaccent_immutable(:search)
    
            OR public.unaccent_immutable(b.notes) % public.unaccent_immutable(:search)
        )
    
        -- Category visibility
        AND category_is_visible(c.id, :viewerId, :isAdmin, :isModeratorOrAdmin, :isVerified)
    
        AND (:categoryId IS NULL OR t.category_id = :categoryId)
        AND (:creatorId IS NULL OR t.creator_id = :creatorId)
        AND (:threadType IS NULL OR t.thread_type = :threadType::thread_type_enum)
        AND (:threadStatus IS NULL OR t.thread_status = :threadStatus::thread_status_enum)
        AND (:hasContentWarning IS NULL OR
                (CASE WHEN :hasContentWarning = true
                    THEN t.content_warning_type != 'NONE'
                    ELSE t.content_warning_type = 'NONE'
                END)
            )
    """)
    Mono<Long> countBookmarksWithFilters(
           @Param("viewerId") UUID viewerId,
           @Param("isAdmin") boolean isAdmin,
           @Param("isModeratorOrAdmin") boolean isModeratorOrAdmin,
           @Param("isVerified") boolean isVerified,
           @Param("categoryId") UUID categoryId,
           @Param("creatorId") UUID creatorId,
           @Param("threadType") String threadType,
           @Param("threadStatus") String threadStatus,
           @Param("hasContentWarning") Boolean hasContentWarning,
           @Param("search") String search
    );

    @Query("""
        SELECT COUNT(*)
        FROM thread_bookmarks b
        INNER JOIN forum_threads t ON b.thread_id = t.id
        INNER JOIN forum_categories c ON t.category_id = c.id
        WHERE b.user_id = :userId
            AND t.is_deleted = false
            AND category_is_visible(c.id, :userId, :isAdmin, :isModeratorOrAdmin, :isVerified)
    """)
    Mono<Long> countVisibleByUserId(
            @Param("userId") UUID userId,
            @Param("isAdmin") boolean isAdmin,
            @Param("isModeratorOrAdmin") boolean isModeratorOrAdmin,
            @Param("isVerified") boolean isVerified
    );

    @Query("""
        SELECT COUNT(*)
        FROM thread_bookmarks b
        INNER JOIN forum_threads t ON b.thread_id = t.id
        INNER JOIN forum_categories c ON t.category_id = c.id
        WHERE b.thread_id = :threadId
            AND t.is_deleted = false
            AND category_is_visible(c.id, :viewerId, :isAdmin, :isModeratorOrAdmin, :isVerified)
    """)
    Mono<Long> countVisibleByThreadId(
            @Param("threadId") UUID threadId,
            @Param("viewerId") UUID viewerId,
            @Param("isAdmin") boolean isAdmin,
            @Param("isModeratorOrAdmin") boolean isModeratorOrAdmin,
            @Param("isVerified") boolean isVerified
    );

}
