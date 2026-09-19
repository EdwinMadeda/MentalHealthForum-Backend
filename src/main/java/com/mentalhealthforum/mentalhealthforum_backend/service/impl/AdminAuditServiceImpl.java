package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;

import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserAuditLogEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserAuditReasonDefinitionEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.UserAuditLogRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.UserAuditReasonDefinitionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for logging admin-initiated user changes to the audit log.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Creating audit log entries for admin actions</li>
 *   <li>Retrieving audit history for a user</li>
 *   <li>Resolving reason definitions and display names</li>
 * </ul>
 *
 * <p><b>Note:</b> This service is only for admin-initiated changes.
 * User-initiated changes (login, password change, profile update)
 * are not logged here.
 */
@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditServiceImpl.class);

    private static final int RECENT_HISTORY_LIMIT = 5;

    private final UserAuditLogRepository auditLogRepository;
    private final UserAuditReasonDefinitionRepository auditReasonDefinitionRepository;
    private final AppUserRepository appUserRepository;


    public AdminAuditServiceImpl(
            UserAuditLogRepository auditLogRepository,
            UserAuditReasonDefinitionRepository auditReasonDefinitionRepository,
            AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.auditReasonDefinitionRepository = auditReasonDefinitionRepository;
        this.appUserRepository = appUserRepository;

    }

    // ============================================================
    // AUDIT LOGGING
    // ============================================================

    // Logs an admin-initiated action for a user.
    @Override
    public Mono<UserAuditLogEntity> logAction(
            UUID userId,
            UserAuditAction action,
            UserAuditSnapshot oldValue,
            UserAuditSnapshot newValue,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        UserAuditLogEntity auditLog = new UserAuditLogEntity(
                userId,
                action,
                oldValue,
                newValue,
                performedBy,
                reasonDefinitionId,
                customReason
        );

        return auditLogRepository.save(auditLog);
    }

    // Convenience method for logging group change
    @Override
    public Mono<UserAuditLogEntity> logGroupChange(
            UUID userId,
            GroupContext context,
            GroupPath oldGroup,
            GroupPath newGroup,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        UserAuditAction action = UserAuditAction.forGroupChange(context, oldGroup, newGroup);

        return logAction(
                userId,
                action,
                UserAuditSnapshot.forGroup(oldGroup),
                UserAuditSnapshot.forGroup(newGroup),
                performedBy,
                reasonDefinitionId,
                customReason
        );

    }

    // Convenience method for logging an enabled status change
    @Override
    public Mono<UserAuditLogEntity> logEnabledChange(
            UUID userId,
            Boolean oldEnabled,
            Boolean newEnabled,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        UserAuditAction action = UserAuditAction.forEnabledChange(newEnabled);

        return logAction(
                userId,
                action,
                UserAuditSnapshot.forEnabled(oldEnabled),
                UserAuditSnapshot.forEnabled(newEnabled),
                performedBy,
                reasonDefinitionId,
                customReason
        );

    }

    // Convenience method for logging a user created
    @Override
    public Mono<UserAuditLogEntity> logUserCreated(
            UUID userId,
            GroupPath group,
            UUID performedBy
    ){
        return logAction(
                userId,
                UserAuditAction.CREATED,
                null,
                UserAuditSnapshot.forGroup(group),
                performedBy,
                null,
                null
        );

    }

    // Convenience method for logging a user created
    @Override
    public Mono<UserAuditLogEntity> logInviteReissued(
            UUID userId,
            GroupPath oldGroup,
            GroupPath newGroup,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        return logAction(
                userId,
                UserAuditAction.INVITE_REISSUED,
                UserAuditSnapshot.forGroup(oldGroup),
                UserAuditSnapshot.forGroup(newGroup),
                performedBy,
                reasonDefinitionId,
                customReason
        );

    }

    // Convenience method for logging a user created
    @Override
    public Mono<UserAuditLogEntity> logInviteRevoked(
            UUID userId,
            UUID performedBy,
            UUID reasonDefinitionId,
            String customReason
    ){
        return logAction(
                userId,
                UserAuditAction.INVITE_REVOKED,
                null,
                null,
                performedBy,
                reasonDefinitionId,
                customReason
        );

    }

    // Convenience method for logging a user created
    @Override
    public Mono<UserAuditLogEntity> logSynced(
            UUID userId,
            GroupPath group,
            UUID performedBy
    ){
        return logAction(
                userId,
                UserAuditAction.SYNCED,
                null,
                UserAuditSnapshot.forGroup(group),
                performedBy,
                null,
                null
        );

    }

    // ============================================================
    // HISTORY RETRIEVAL
    // ============================================================

    // Retrieves the full audit history for a user, ordered newest first.
    @Override
    public Flux<UserHistoryEntry> getUserHistoryList(UUID userId){
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .flatMapSequential(this::mapToHistoryEntry); // Preserves order
    }

    // Retrieves the most recent N audit history entries for a user.
    @Override
    public Flux<UserHistoryEntry> getRecentUserHistory(UUID userId){
        return auditLogRepository.findRecentByUserId(userId, RECENT_HISTORY_LIMIT)
                .flatMapSequential(this::mapToHistoryEntry); // Preserves order
    }

    private Mono<UserHistoryEntry> mapToHistoryEntry(UserAuditLogEntity userAuditLog) {

        // Handle null performedBy using defaultUser()
        Mono<UserDetails> performedByDetailsMono = userAuditLog.getPerformedBy() != null
                ? appUserRepository.findAppUserByKeycloakId(userAuditLog.getPerformedBy().toString())
                    .map(AppUserEntity::toUserDetails)
                      .defaultIfEmpty(AppUserEntity.defaultUser())
                : Mono.just(AppUserEntity.defaultUser());

        // Resolve suggested reason (if reasonDefinitionId is present)
        Mono<UserAuditReasonDefinitionEntity> reasonMono = userAuditLog.getReasonDefinitionId() != null
                ? auditReasonDefinitionRepository.findById(userAuditLog.getReasonDefinitionId())
                  .defaultIfEmpty(new UserAuditReasonDefinitionEntity())
                : Mono.just(new UserAuditReasonDefinitionEntity());

        return Mono.zip(performedByDetailsMono, reasonMono)
                .map(tuple -> {
                    UserDetails userDetails = tuple.getT1();
                    UserAuditReasonDefinitionEntity reason = tuple.getT2();

                    UserAuditReasonDefinitionDto suggestedReason = toUserAuditReasonDefinitionDto(reason);

                    return new UserHistoryEntry(
                            userAuditLog.getActionType(),
                            userAuditLog.getOldValue(),
                            userAuditLog.getNewValue(),
                            userAuditLog.getPerformedBy(),
                            userDetails.getDisplayName(),
                            suggestedReason,
                            userAuditLog.getCustomReason(),
                            userAuditLog.getCreatedAt()
                    );
                });

    }

    // ============================================================
    // REASON DEFINITION RETRIEVAL
    // ============================================================

    //  Retrieves active reason definitions for a specific action type.
    @Override
    public Flux<UserAuditReasonDefinitionDto> getReasonDefinition(UserAuditAction actionType){
        return auditReasonDefinitionRepository
                .findByActionTypeAndIsActiveTrueOrderBySortOrderAsc(actionType)
                .map(this::toUserAuditReasonDefinitionDto);
    }

    // Retrieves a reason definition by its key.
    @Override
    public Mono<UserAuditReasonDefinitionDto> getReasonDefinition(UserAuditReasonKey key){
        return auditReasonDefinitionRepository
                .findByKey(key)
                .map(this::toUserAuditReasonDefinitionDto);
    }

    // Retrieves all active reason definitions.
    @Override
    public Mono<List<UserAuditReasonDefinitionGroupedDto>> getAllReasonDefinitions(){
        return auditReasonDefinitionRepository
                .findByIsActiveTrueOrderByActionTypeAscSortOrderAsc()
                .collectList()
                .map(this::groupByActionType);
    }

    private List<UserAuditReasonDefinitionGroupedDto> groupByActionType(List<UserAuditReasonDefinitionEntity> entities){
        return entities.stream()
                .collect(Collectors.groupingBy(UserAuditReasonDefinitionEntity::getActionType))
                .entrySet().stream()
                .map(entry -> new UserAuditReasonDefinitionGroupedDto(
                        entry.getKey(),
                        entry.getKey().getDisplayName(),
                        entry.getValue().stream()
                                .map(this::toUserAuditReasonDefinitionDto)
                                .toList()
                ))
                .sorted(Comparator.comparing(group -> group.actionType().ordinal()))
                .toList();

    }

    private UserAuditReasonDefinitionDto toUserAuditReasonDefinitionDto(UserAuditReasonDefinitionEntity entity){
        if(entity == null || entity.getId() == null){
            return null;
        }

        return new UserAuditReasonDefinitionDto(
                 entity.getId(),
                 entity.getKey(),
                 entity.getDescription(),
                 entity.getActionType(),
                entity.getIsActive(),
                entity.getSortOrder()
        );

    }


}
