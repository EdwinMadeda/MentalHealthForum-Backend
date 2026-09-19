package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserType;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserAuditLogEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AdminAuditService {
    // Logs an admin-initiated action for a user.
    Mono<UserAuditLogEntity> logAction(
            UUID userId,
            UserAuditAction action,
            UserAuditSnapshot oldValue,
            UserAuditSnapshot newValue,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    );

    // Convenience method for logging group change
    Mono<UserAuditLogEntity> logGroupChange(
            UUID userId,
            GroupContext context,
            GroupPath oldGroup,
            GroupPath newGroup,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    );

    // Convenience method for logging an enabled status change
    Mono<UserAuditLogEntity> logEnabledChange(
            UUID userId,
            Boolean oldEnabled,
            Boolean newEnabled,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    );

    // Convenience method for logging a user created
    Mono<UserAuditLogEntity> logUserCreated(
            UUID userId,
            GroupPath group,
            UUID performedBy
    );

    // Convenience method for logging a user created
    Mono<UserAuditLogEntity> logInviteReissued(
            UUID userId,
            GroupPath oldGroup,
            GroupPath newGroup,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    );

    // Convenience method for logging a user created
    Mono<UserAuditLogEntity> logInviteRevoked(
            UUID userId,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    );

    // Convenience method for logging a user created
    Mono<UserAuditLogEntity> logSynced(
            UUID userId,
            GroupPath group,
            UUID performedBy
    );

    // Retrieves the full audit history for a user, ordered newest first.
    Flux<UserHistoryEntry> getUserHistoryList(UUID userId);

    // Retrieves the most recent N audit history entries for a user.
    Flux<UserHistoryEntry> getRecentUserHistory(UUID userId);

    //  Retrieves active reason definitions for a specific action type.
    Flux<UserAuditReasonDefinitionDto> getReasonDefinition(UserAuditAction actionType);

    // Retrieves a reason definition by its key.
    Mono<UserAuditReasonDefinitionDto> getReasonDefinition(UserAuditReasonKey key);

    // Retrieves all active reason definitions.
    Mono<List<UserAuditReasonDefinitionGroupedDto>> getAllReasonDefinitions();
}
