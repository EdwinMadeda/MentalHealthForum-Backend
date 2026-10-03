package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.UserHistoryFilterDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;

import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.UserHistorySortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
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

import java.util.*;
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
public class UserAuditServiceImpl implements UserAuditService {

    private static final Logger log = LoggerFactory.getLogger(UserAuditServiceImpl.class);

    private static final int RECENT_HISTORY_LIMIT = 5;

    private final UserAuditLogRepository auditLogRepository;
    private final UserAuditReasonDefinitionRepository auditReasonDefinitionRepository;
    private final AppUserRepository appUserRepository;


    public UserAuditServiceImpl(
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

    // Retrieves the most recent N audit history entries for a user.
    @Override
    public Flux<UserHistoryEntry> getRecentUserHistory(UUID userId){
        return auditLogRepository.findRecentByUserId(userId, RECENT_HISTORY_LIMIT)
                .collectList()
                .flatMapMany(records -> enrichedHistoryWithBatchData(records)
                        .flatMapMany(enriched -> Flux.fromIterable(enriched.entries))
                );
    }

    // Retrieves the full audit history for a user, ordered newest first.
    @Override
    public Flux<UserHistoryEntry> getUserHistoryList(UUID userId){
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .collectList()
                .flatMapMany(records -> enrichedHistoryWithBatchData(records)
                        .flatMapMany(enriched -> Flux.fromIterable(enriched.entries)));
    }

    @Override
    public Mono<PaginatedResponse<UserHistoryEntry>> getUserHistoryPaginated(
            int page,
            int size,
            UUID userId,
            UUID performedBy,
            UserAuditAction[] actionTypes,
            String search,
            UserHistorySortField sortBy,
            String sortDirection,
            ViewerContext viewerContext){

        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters (getUserHistoryPaginated) : page={}, size={}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        String [] effectiveActionTypes = (actionTypes == null || actionTypes.length == 0)
                ? null
                : Arrays.stream(actionTypes).map(Enum::name).toArray(String[]::new);

        String effectiveSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();

        UserHistorySortField sortByField = sortBy != null ? sortBy : UserHistorySortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);

        return auditLogRepository.findByUserIdPaginated(userId, performedBy, effectiveActionTypes, effectiveSearch, effectiveSortDirection, size, offset)
                .collectList()
                .flatMap(records -> {
                    if(records.isEmpty()){
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, 0L));
                    }

                    return enrichedHistoryWithBatchData(records)
                            .zipWith(auditLogRepository.countUserHistoryWithFilters(userId, performedBy, effectiveActionTypes, effectiveSearch))
                            .map(tuple -> {
                                EnrichedHistoryData enriched = tuple.getT1();
                                long totalCount = tuple.getT2();

                                FilterMetadata<UserHistoryFilterDto> filters = buildUserHistoryFilters(enriched);

                                return new PaginatedResponse<>(
                                        enriched.entries,
                                        page,
                                        size,
                                        totalCount,
                                        filters
                                );
                            });

                });


    }

    @Override
    public Mono<PaginatedResponse<UserHistoryEntry>> getMyHistoryPaginated(
            int page,
            int size,
            UserHistorySortField sortBy,
            String sortDirection,
            ViewerContext viewerContext){

        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters (getMyHistoryPaginated) : page={}, size={}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        UUID userId = UUID.fromString(viewerContext.getUserId());

        UserHistorySortField sortByField = sortBy != null ? sortBy : UserHistorySortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);

        return auditLogRepository.findByUserIdPaginated(userId, null, null, null, effectiveSortDirection, size, offset)
                .collectList()
                .flatMap(records -> {
                    if(records.isEmpty()){
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, 0L));
                    }

                    return enrichedHistoryWithBatchData(records)
                            .zipWith(auditLogRepository.countUserHistoryWithFilters(userId, null, null, null))
                            .map(tuple -> {
                                EnrichedHistoryData enriched = tuple.getT1();
                                long totalCount = tuple.getT2();

                                // Anonymize for user view
                                List<UserHistoryEntry> anonymizedEntries = enriched.entries.stream()
                                            .map(this::anonymizeForUser)
                                            .toList();

                                return new PaginatedResponse<>(
                                        anonymizedEntries,
                                        page,
                                        size,
                                        totalCount
                                        // No filters for user view
                                );
                            });

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

    /**
     * Enriches a list of audit log records with performer and reason details using batch fetching.
     * Uses batch fetching to avoid N+1 queries.
     */
    private Mono<EnrichedHistoryData> enrichedHistoryWithBatchData(List<UserAuditLogEntity> records){
        if(records.isEmpty()){
            return Mono.just(new EnrichedHistoryData(
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of()
            ));
        }

        // Collect unique performedBy IDs
        List<UUID> performedByIds = records.stream()
                .map(UserAuditLogEntity::getPerformedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Collect unique targetUser IDs
        List<UUID> targetUserIds = records.stream()
                .map(UserAuditLogEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Collect unique reason definition IDs
        List<UUID> reasonIds = records.stream()
                .map(UserAuditLogEntity::getReasonDefinitionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch performedBys
        Mono<Map<UUID, UserDetails>> performedBysMono = performedByIds.isEmpty()
                ? Mono.just(Map.of())
                : appUserRepository.findAppUsersByKeycloakIds(performedByIds)
                  .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                  .defaultIfEmpty(new HashMap<>());

        // Batch fetch targetUsers
        Mono<Map<UUID, UserDetails>> targetUsersMono = targetUserIds.isEmpty()
                ? Mono.just(Map.of())
                : appUserRepository.findAppUsersByKeycloakIds(targetUserIds)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());

        // Batch fetch reasons
        Mono<Map<UUID, UserAuditReasonDefinitionEntity>> reasonsMono = reasonIds.isEmpty()
                ? Mono.just(Map.of())
                : auditReasonDefinitionRepository.findAuditReasonDefinitionByIds(reasonIds)
                  .collectMap(UserAuditReasonDefinitionEntity::getId)
                  .defaultIfEmpty(new HashMap<>());

        return Mono.zip(performedBysMono, targetUsersMono, reasonsMono)
                .map(tuple -> {
                    Map<UUID, UserDetails> performedBys = tuple.getT1();
                    Map<UUID, UserDetails> targetUsers = tuple.getT2();
                    Map<UUID, UserAuditReasonDefinitionEntity> reasons = tuple.getT3();

                    List<UserHistoryEntry> entries = records.stream()
                            .map(record -> mapToHistoryEntryWithData(record, performedBys, targetUsers, reasons))
                            .toList();

                    return new EnrichedHistoryData(entries, records, performedBys, targetUsers, reasons);
                });

    }

    /**
     * Internal carrier for batch-enriched history data.
     * Holds both the enriched responses and the raw data needed for future filters.
     */
    private record EnrichedHistoryData(
       List<UserHistoryEntry> entries,
       List<UserAuditLogEntity> records,
       Map<UUID, UserDetails> performedBys,
       Map<UUID, UserDetails> targetUsers,
       Map<UUID, UserAuditReasonDefinitionEntity> reasons
    ){}


    /**
     * Maps an audit log entity to a UserHistoryEntry using pre-fetched data.
     * This is synchronous - no async calls needed.
     */
    private UserHistoryEntry mapToHistoryEntryWithData(
            UserAuditLogEntity record,
            Map<UUID, UserDetails> performedBys,
            Map<UUID, UserDetails> targetUsers,
            Map<UUID, UserAuditReasonDefinitionEntity> reasons) {

        // Get performer (or default)
        UserDetails performedBy = record.getPerformedBy() != null
                ? performedBys.getOrDefault(record.getPerformedBy(), AppUserEntity.defaultUser())
                : AppUserEntity.defaultUser();

        // Get target user (or default)
        UserDetails targetUser = record.getUserId() != null
                ? targetUsers.getOrDefault(record.getUserId(), AppUserEntity.defaultUser())
                : AppUserEntity.defaultUser();

        // Get reason definition (or null)
        UserAuditReasonDefinitionEntity reasonDefinition = record.getReasonDefinitionId() != null
                ? reasons.get(record.getReasonDefinitionId())
                : null;

        UserAuditReasonDefinitionDto suggestedReason = toUserAuditReasonDefinitionDto(reasonDefinition);

        return new UserHistoryEntry(
                record.getActionType(),
                record.getOldValue(),
                record.getNewValue(),
                performedBy,
                targetUser,
                suggestedReason,
                record.getCustomReason(),
                record.getCreatedAt()
        );

    }

    private FilterMetadata<UserHistoryFilterDto> buildUserHistoryFilters(EnrichedHistoryData data){
        // Build action type options
        Map<UserAuditAction, Long> actionCounts = data.records().stream()
                .collect(Collectors.groupingBy(
                        UserAuditLogEntity::getActionType,
                        Collectors.counting()
                ));

        List<FilterOption> actionOptions = actionCounts.entrySet().stream()
                .map(entry -> new FilterOption(
                        entry.getKey().getDisplayName(),
                        entry.getKey().name(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build target user options (filter nulls)
        Map<UUID, Long> targetUserCounts = data.records().stream()
                .filter(record -> record.getUserId() != null)
                .collect(Collectors.groupingBy(
                        UserAuditLogEntity::getUserId,
                        Collectors.counting()
                ));

        List<FilterOption> targetUserOptions = data.targetUsers().entrySet().stream()
                .map(entry -> {
                    UUID targetUserId = entry.getKey();
                    UserDetails targetUserDetails = entry.getValue();
                    long count = targetUserCounts.getOrDefault(targetUserId, 0L);

                    return new FilterOption(
                            targetUserId,
                            targetUserDetails.getDisplayName(),
                            targetUserId.toString(),
                            targetUserDetails.getAvatarUrl(),
                            count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build performedBy options (filter nulls)
        Map<UUID, Long> performerCounts = data.records().stream()
                .filter(record -> record.getPerformedBy() != null)
                .collect(Collectors.groupingBy(
                        UserAuditLogEntity::getPerformedBy,
                        Collectors.counting()
                ));

        List<FilterOption> performedByOptions = data.performedBys().entrySet().stream()
                .map(entry -> {
                    UUID performedById = entry.getKey();
                    UserDetails performedByDetails = entry.getValue();
                    long count = performerCounts.getOrDefault(performedById, 0L);

                    return new FilterOption(
                            performedById,
                            performedByDetails.getDisplayName(),
                            performedById.toString(),
                            performedByDetails.getAvatarUrl(),
                            count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        UserHistoryFilterDto userHistoryFilters = UserHistoryFilterDto.builder()
                .auditActions(actionOptions)
                .performedBys(performedByOptions)
                .targetUsers(targetUserOptions)
                .build();

        return FilterMetadata.<UserHistoryFilterDto>builder()
                .filters(userHistoryFilters)
                .sortOptions(getUserHistorySortOptions())
                .build();

    }

    /**
     * Anonymizes a history entry for user self-view.
     * <p>Hides:
     * <ul>
     *   <li>Performer identity (ID, name, avatar)</li>
     *   <li>Custom reason (could contain internal notes)</li>
     * </ul>
     */
    private UserHistoryEntry anonymizeForUser(UserHistoryEntry entry){

        UserDetails anonymizedPerformedBy = UserDetails.builder()
                .userId(null)
                .displayName("Admin")
                .avatarUrl(null)
                .initials(null)
                .lastActiveAt(null)
                .build();

        return new UserHistoryEntry(
                entry.action(),
                entry.oldValue(),
                entry.newValue(),
                anonymizedPerformedBy,               //  Hide performedBy
                entry.targetUser(),
                entry.suggestedReason(),
                null,              // Hide customReason
                entry.timeStamp()
        );
    }


    private List<SortOption> getUserHistorySortOptions(){
        return Arrays.stream(
                UserHistorySortField.values())
                .map(UserHistorySortField::toSortOption)
                .toList();
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
