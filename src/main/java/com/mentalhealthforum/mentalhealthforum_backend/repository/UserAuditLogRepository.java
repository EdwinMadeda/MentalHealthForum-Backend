package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserAuditLogEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserAuditLogRepository extends R2dbcRepository<UserAuditLogEntity, UUID> {

    // Find all audit log entries for a specific user, ordered by newest first.
    // This is the primary query for displaying the user's history
    @Query("SELECT * FROM user_audit_log WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<UserAuditLogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Find all audit log entries for a specific user and action type.
    Flux<UserAuditLogEntity> findByUserIdAndActionTypeOrderByCreatedAtDesc(UUID userId, UserAuditAction actionType);

    // Finds the most recent N audit log entries for a specific user
    @Query("SELECT * FROM user_audit_log WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    Flux<UserAuditLogEntity> findRecentByUserId(@Param("userId") UUID userId, @Param("limit") int limit);


    // Find the most recent audit log entry for a specific user.
    // Useful for getting the user's current state or last action
    Mono<UserAuditLogEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);


    // Count number of audit entries for a specific user
    Mono<Long> countByUserId(UUID userId);


    // Deletes all audit log entries for a specific user.
    // Used when user is permanently purged (if needed)
    Mono<Void> deleteByUserId(UUID userId);
}
