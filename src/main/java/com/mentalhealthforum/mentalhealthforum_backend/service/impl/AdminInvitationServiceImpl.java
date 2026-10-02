package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.PendingInviteFilterDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.PendingAdminInviteResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.PendingInviteSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.UserDoesNotExistException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AdminInvitationEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AdminInvitationRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.VerificationTokenRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AdminInvitationService;
import com.mentalhealthforum.mentalhealthforum_backend.service.KeycloakAdminManager;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mentalhealthforum.mentalhealthforum_backend.utils.ChangeUtils.setIfChanged;


@Service
public class AdminInvitationServiceImpl implements AdminInvitationService {

    private static final Logger log = LoggerFactory.getLogger(AdminInvitationServiceImpl.class);

    private final KeycloakAdminManager adminManager;
    private final AdminInvitationRepository adminInvitationRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AppUserRepository appUserRepository;

    public AdminInvitationServiceImpl(
            KeycloakAdminManager adminManager,
            AdminInvitationRepository adminInvitationRepository,
            VerificationTokenRepository verificationTokenRepository,
            AppUserRepository appUserRepository) {
        this.adminManager = adminManager;
        this.adminInvitationRepository = adminInvitationRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Mono<AdminInvitationEntity> createInvitation(KeycloakUserDto keycloakUserDto, String invitedById){
        List<String> groups = adminManager.getUserGroups(keycloakUserDto.userId());

        AdminInvitationEntity adminInvitation = new AdminInvitationEntity(
                keycloakUserDto.userId(),
                keycloakUserDto.email(),
                keycloakUserDto.username(),
                keycloakUserDto.firstName(),
                keycloakUserDto.lastName(),
                new HashSet<>(groups),
                keycloakUserDto.getCreatedInstant(),
                invitedById
        );
        return adminInvitationRepository.save(adminInvitation);
    }

    @Override
    public Mono<PendingAdminInviteResponse> updateInvitation(KeycloakUserDto keycloakUserDto){
        List<String> groups = adminManager.getUserGroups(keycloakUserDto.userId());
        return adminInvitationRepository.findByKeycloakId(UUID.fromString(keycloakUserDto.userId()))
                .flatMap(existing -> {
                    // Update the fields to match the current Keycloak state
                    existing.setEmail(keycloakUserDto.email());
                    existing.setUsername(keycloakUserDto.username());
                    existing.setFirstName(keycloakUserDto.firstName());
                    existing.setLastName(keycloakUserDto.lastName());
                    existing.setIsEnabled(keycloakUserDto.enabled());
                    existing.setIsEmailVerified(keycloakUserDto.emailVerified());
                    existing.setUpdatedAt(Instant.now());
                    existing.setGroups(new HashSet<>(groups));

                    return adminInvitationRepository.save(existing)
                            .flatMap(this::enrichSingleInviteWithData);
                })
                // If they aren't in the lobby, we just return empty so the chain continues
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<PendingAdminInviteResponse> syncPendingInviteFromKeycloak(String userId){
        return adminInvitationRepository.findByKeycloakId(UUID.fromString(userId))
                .switchIfEmpty(Mono.error(new UserDoesNotExistException(
                        "User not found in pending invitations."
                )))
                .flatMap(invitation ->
                        Mono.fromCallable(()-> adminManager.findUserByUserId(userId)
                                        .orElseThrow(UserDoesNotExistException::new))
                                .subscribeOn(Schedulers.boundedElastic())
                                .zipWith(Mono.fromCallable(()-> adminManager.getUserGroups(userId))
                                        .subscribeOn(Schedulers.boundedElastic()))
                                .flatMap(tuple -> {

                                    UserRepresentation userRep = tuple.getT1();
                                    Set<String> keycloakGroups = new HashSet<>(tuple.getT2());

                                    boolean hasChanges = false;

                                    // Check email change
                                    hasChanges |= setIfChanged(
                                            userRep.getEmail(),
                                            invitation.getEmail(),
                                            invitation::setEmail
                                    );

                                    // Check username change
                                    hasChanges |= setIfChanged(
                                            userRep.getUsername(),
                                            invitation.getUsername(),
                                            invitation::setUsername
                                    );

                                    // Check firstname change
                                    hasChanges |= setIfChanged(
                                            userRep.getFirstName(),
                                            invitation.getFirstName(),
                                            invitation::setFirstName
                                    );

                                    // Check lastname change
                                    hasChanges |= setIfChanged(
                                            userRep.getLastName(),
                                            invitation.getLastName(),
                                            invitation::setLastName
                                    );

                                    // Check enabled change
                                    hasChanges |= setIfChanged(
                                            userRep.isEnabled(),
                                            invitation.getIsEnabled(),
                                            invitation::setIsEnabled
                                    );

                                    // Check verified change
                                    hasChanges |= setIfChanged(
                                            userRep.isEmailVerified(),
                                            invitation.getIsEmailVerified(),
                                            invitation::setIsEmailVerified
                                    );

                                    // Check groups change
                                    if(!keycloakGroups.equals(invitation.getGroups())){
                                        invitation.setGroups(keycloakGroups);
                                        hasChanges = true;
                                    }

                                    if(hasChanges){
                                        invitation.setUpdatedAt(Instant.now());
                                        log.info("Synced pending invite for user {} with fresh keycloak data", userId);

                                        return adminInvitationRepository.save(invitation);
                                    }

                                    log.debug("No changes detected for pending invite {}", userId);
                                    return Mono.just(invitation);

                                })
                )
                .flatMap(this::enrichSingleInviteWithData);
    }

    @Override
    public Mono<Void> processVerificationSuccess(String userId){
        return adminInvitationRepository.markEmailVerifiedAndAdvanceStage(UUID.fromString(userId))
                .doOnSuccess(count -> {
                    if(count > 0){
                        log.info("User {} successfully verified email and moved to PASSWORD_RESET stage.", userId);
                    }
                })
                .then();
    }

    @Override
    public Mono<Void> processPasswordResetSuccess(String userId){
        return adminInvitationRepository.invalidateOneTimePass(UUID.fromString(userId))
                .then(adminInvitationRepository.updateStage(UUID.fromString(userId), OnboardingStage.AWAITING_PROFILE_COMPLETION))
                .then();
    }


    @Override
    public Mono<Void> updateOnboardingStage(String userId, OnboardingStage onboardingStage){
        return adminInvitationRepository.updateStage(UUID.fromString(userId), onboardingStage)
                .doOnSuccess(v-> log.info("User {} moved to {}", userId, onboardingStage.name()))
                .then();
    }

    @Override
    public Mono<PendingAdminInviteResponse> getPendingInvite(String userId){
        return syncPendingInviteFromKeycloak(userId);
    }

    @Override
    public Mono<PaginatedResponse<PendingAdminInviteResponse>> getPendingInvites(
            int page,
            int size,
            GroupPath[] groups,
            UUID invitedByUserId,
            String search,
            OnboardingStage onboardingStage,
            PendingInviteSortField sortBy,
            String sortDirection) {

        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters: page={}, size={}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        // Convert GroupPath[] to String [] (paths)
        String[] effectiveGroups = (groups == null || groups.length == 0)
                ? null
                : Arrays.stream(groups)
                  .map(GroupPath::getPath)
                  .toArray(String[]::new);

        String effectiveOnboardingStage = onboardingStage != null ? onboardingStage.name() : null;
        String effectiveSearch = (search == null || search.trim().isEmpty()) ? null: search.trim();

        PendingInviteSortField sortByField = sortBy != null ? sortBy : PendingInviteSortField.DEFAULT;
        String normalizedSortDirection = sortByField.determineSortDirection(sortDirection);

        return adminInvitationRepository.findPendingInvitesPaginated(
                        invitedByUserId,
                        effectiveGroups,
                        effectiveOnboardingStage,
                        effectiveSearch,
                        sortByField.getValue(),
                        normalizedSortDirection,
                        size,
                        offset
                )
                .collectList()
                .flatMap(records -> {
                    if(records.isEmpty()){
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, 0L));
                    }

                    return enrichPendingInvitesWithBatchData(records)
                            .zipWith(adminInvitationRepository.countPendingInvitesWithFilters(
                                    invitedByUserId,
                                    effectiveGroups,
                                    effectiveOnboardingStage,
                                    effectiveSearch
                            ))
                            .map(tuple -> {
                                EnrichedPendingInviteData enriched = tuple.getT1();
                                long totalCount = tuple.getT2();

                                FilterMetadata<PendingInviteFilterDto> filters = buildPendingInviteFilters(enriched);

                                return new PaginatedResponse<>(enriched.invites, page, size, totalCount, filters);
                            });
                });

    }

    @Override
    public Mono<Void> completeInvitation(UUID keycloakId){
        log.info("Completing invitation lifecycle for: {}. Removing from Lobby and clearing tokens.", keycloakId);

        // Fetch from our Lobby first (faster than hitting Keycloak)
        return adminInvitationRepository.findByKeycloakId(keycloakId)
                .flatMap(adminInvitation -> {
                    // Delete tokens based on the email from our Lobby record
                    return verificationTokenRepository.deleteByEmail(adminInvitation.getEmail())
                            .then(adminInvitationRepository.delete(adminInvitation));
                })
                .doOnSuccess(v -> log.info("Lobby record for {} successfully cleared.", keycloakId))
                .then();
    }

    @Override
    public Mono<Void> purgeExpiredInvitation(AdminInvitationEntity invitation){
        log.info("Purging expired invitation: {}", invitation.getKeycloakId());

        return Mono.fromCallable(()-> {
            adminManager.deleteUser(invitation.getKeycloakId().toString());
            return invitation.getKeycloakId();
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::completeInvitation)
                .onErrorResume(error -> {
                    log.error("Failed to purge expired invitation: {}", invitation.getKeycloakId(), error);
                    return Mono.empty();
                });
    }

    private Mono<PendingAdminInviteResponse> enrichSingleInviteWithData(AdminInvitationEntity entity){
        if (entity == null) {
            return Mono.empty();
        }

        return appUserRepository.findAppUserByKeycloakId(entity.getInvitedBy().toString())
                .map(AppUserEntity::toUserDetails)
                .map(invitedBy -> mapResponseWithData(entity, invitedBy));

    }

    private Mono<EnrichedPendingInviteData> enrichPendingInvitesWithBatchData(
            List<AdminInvitationEntity> records
    ) {
        if (records.isEmpty()) {
            return Mono.just(new EnrichedPendingInviteData(
                    List.of(),
                    List.of(),
                    Map.of()
            ));
        }

        // Extract unique inviter IDs
        List<UUID> inviterIds = records.stream()
                .map(AdminInvitationEntity::getInvitedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch inviters
        Mono<Map<UUID, UserDetails>> invitersMono = inviterIds.isEmpty()
                ? Mono.just(Map.of())
                : appUserRepository.findAppUsersByKeycloakIds(inviterIds)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());

        return invitersMono.map(inviters -> {

            List<PendingAdminInviteResponse> invites = records.stream()
                    .map(record -> {
                        UserDetails invitedBy = record.getInvitedBy() != null
                                ? inviters.getOrDefault(record.getInvitedBy(), AppUserEntity.defaultUser())
                                : AppUserEntity.defaultUser();

                        return mapResponseWithData(record, invitedBy);
                    })
                    .toList();


            return new EnrichedPendingInviteData(invites, records, inviters);
        });
    }

    private PendingAdminInviteResponse mapResponseWithData(
            AdminInvitationEntity entity,
            UserDetails invitedBy
    ){

        boolean isEnabled = entity.getIsEnabled() != null ? entity.getIsEnabled() : false;
        boolean isEmailVerified = entity.getIsEmailVerified() != null ? entity.getIsEmailVerified() : false;

        return PendingAdminInviteResponse.builder()
                .userId(entity.getKeycloakId())
                .username(entity.getUsername())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .groups(entity.getGroupPaths())
                .isEnabled(isEnabled)
                .isEmailVerified(isEmailVerified)
                .invitedBy(entity.getInvitedBy())
                .invitedByDisplayName(invitedBy.getDisplayName())
                .invitedByAvatarUrl(invitedBy.getAvatarUrl())
                .dateCreated(entity.getDateCreated())
                .updatedAt(entity.getUpdatedAt())
                .currentStage(entity.getCurrentStage())
                .expiresAt(entity.getExpiresAt())
                .isExpired(entity.isExpired())
                .isEligibleForPurge(entity.isEligibleForPurge())
                .build();

    }


    private record EnrichedPendingInviteData(
            List<PendingAdminInviteResponse> invites,
            List<AdminInvitationEntity> records,
            Map<UUID, UserDetails> inviters
    ) {}

    private FilterMetadata<PendingInviteFilterDto> buildPendingInviteFilters(
            EnrichedPendingInviteData data){

        // Build stage options
        Map<OnboardingStage, Long> stageCounts = data.records().stream()
                .collect(Collectors.groupingBy(
                        AdminInvitationEntity::getCurrentStage,
                        Collectors.counting()
                ));

        List<FilterOption> stageOptions = stageCounts.entrySet().stream()
                .map(entry -> new FilterOption(
                        entry.getKey().getDisplayName(),
                        entry.getKey().name(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build inviter options
        Map<UUID, Long> inviterCounts = data.records().stream()
                .filter(record -> record.getInvitedBy() != null)
                .collect(Collectors.groupingBy(
                        AdminInvitationEntity::getInvitedBy,
                        Collectors.counting()
                ));

        List<FilterOption> inviterOptions = data.inviters().entrySet().stream()
                .map(entry -> {
                    UUID inviterId = entry.getKey();
                    UserDetails inviter = entry.getValue();
                    long count = inviterCounts.getOrDefault(inviterId, 0L);
                    return new FilterOption(
                            inviterId,
                            inviter.getDisplayName(),
                            inviterId.toString(),
                            inviter.getAvatarUrl(),
                            count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build group options
        Map<GroupPath, Long> groupCounts = data.invites().stream()
                .flatMap(invite -> Arrays.stream(invite.getGroups()))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        List<FilterOption> groupOptions = groupCounts.entrySet().stream()
                .map(entry -> new FilterOption(
                        entry.getKey().getDisplayName(),
                        entry.getKey().name(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        PendingInviteFilterDto filters = PendingInviteFilterDto.builder()
                .stages(stageOptions)
                .inviters(inviterOptions)
                .groups(groupOptions)
                .build();

        return FilterMetadata.<PendingInviteFilterDto>builder()
                .filters(filters)
                .sortOptions(getPendingInviteSortOptions())
                .build();

    }

    private List<SortOption> getPendingInviteSortOptions(){
        return Arrays.stream(PendingInviteSortField.values())
                .map(PendingInviteSortField::toSortOption)
                .toList();
    }

}


