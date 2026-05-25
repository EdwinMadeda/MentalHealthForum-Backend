package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadEditHistoryEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ThreadEditHistoryRepository extends R2dbcRepository<ThreadEditHistoryEntity, UUID> {

    @Query("SELECT * FROM thread_edit_history WHERE thread_id = :threadId ORDER BY edited_at DESC")
    Flux<ThreadEditHistoryEntity> findByThreadIdOrderByEditedAtDesc(@Param("threadId") UUID threadId);

    @Query("DELETE FROM thread_edit_history WHERE thread_id = :threadId")
    Mono<Void> deleteByThreadId(@Param("threadId") UUID threadId);
}
