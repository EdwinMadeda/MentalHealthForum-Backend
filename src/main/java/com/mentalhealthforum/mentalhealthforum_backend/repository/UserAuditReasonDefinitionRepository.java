package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserAuditReasonDefinitionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserAuditReasonDefinitionRepository extends R2dbcRepository<UserAuditReasonDefinitionEntity, UUID> {

    // Finds all active reason definitions for a specific action type
    Flux<UserAuditReasonDefinitionEntity> findByActionTypeAndIsActiveTrueOrderBySortOrderAsc(UserAuditAction actionType);

    // Finds a reason definition by its key.
    Mono<UserAuditReasonDefinitionEntity> findByKey(UserAuditReasonKey key);

    // Finds all active reason definitions.
    Flux<UserAuditReasonDefinitionEntity> findByIsActiveTrueOrderByActionTypeAscSortOrderAsc();

    // Checks if a reason definition exists by key.
    Mono<Boolean> existsByKey(UserAuditReasonKey key);


}
