package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.PendingAdminInviteDto;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.PendingInviteSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AdminInvitationEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AdminInvitationRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.VerificationTokenRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AdminInvitationService;
import com.mentalhealthforum.mentalhealthforum_backend.service.KeycloakAdminManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.r2dbc.core.DatabaseClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import java.util.UUID;


@Service
public class AdminInvitationServiceImpl implements AdminInvitationService {

    private static final Logger log = LoggerFactory.getLogger(AdminInvitationServiceImpl.class);

    private final KeycloakAdminManager adminManager;
    private final AdminInvitationRepository adminInvitationRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final DatabaseClient databaseClient;

    public AdminInvitationServiceImpl(
            KeycloakAdminManager adminManager,
            AdminInvitationRepository adminInvitationRepository,
            VerificationTokenRepository verificationTokenRepository,
            DatabaseClient databaseClient) {
        this.adminManager = adminManager;
        this.adminInvitationRepository = adminInvitationRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.databaseClient = databaseClient;
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
    public Mono<AdminInvitationEntity> updateInvitation(KeycloakUserDto keycloakUserDto){
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

                    return adminInvitationRepository.save(existing);
                })
                // If they aren't in the lobby, we just return empty so the chain continues
                .switchIfEmpty(Mono.empty());
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
    public Mono<PaginatedResponse<PendingAdminInviteDto>> getPendingInvites(
            int page,
            int size,
            String[] groups,
            UUID invitedByUserId,
            String search, OnboardingStage onboardingStage, String sortBy,
            String sortDirection) {

        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters: page={}, size={}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        String[] effectiveGroups = (groups == null || groups.length == 0) ? null : groups;
        String effectiveOnboardingStage = onboardingStage != null ? onboardingStage.name() : null;
        String effectiveSearch = (search == null || search.trim().isEmpty()) ? null: search.trim();
        PendingInviteSortField sortByField = PendingInviteSortField.fromString(sortBy);
        String normalizedSortDirection = sortByField.determineSortDirection(sortDirection);

        Flux<PendingAdminInviteDto> pendingInviteFlux = adminInvitationRepository.findPendingInvitesPaginated(
                invitedByUserId,
                effectiveGroups,
                effectiveOnboardingStage,
                effectiveSearch,
                sortByField.getValue(),
                normalizedSortDirection,
                size,
                offset
        );

        Mono<Long> totalCount = adminInvitationRepository.countPendingInvitesWithFilters(
                invitedByUserId,
                effectiveGroups,
                effectiveOnboardingStage,
                effectiveSearch
        );


        return Mono.zip(pendingInviteFlux.collectList(), totalCount)
                .map(tuple -> {
                    List<PendingAdminInviteDto> adminInvites = tuple.getT1();
                    Long total = tuple.getT2();

                    if(adminInvites.isEmpty()){
                        return new PaginatedResponse<>(List.of(), page, size, 0L);
                    }

                    FilterMetadata<Object> filters = FilterMetadata.builder()
                            .sortOptions(getPendingInviteSortOptions())
                            .build();

                    return new PaginatedResponse<>(adminInvites, page, size, total, filters);
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

    private List<SortOption> getPendingInviteSortOptions(){
        return Arrays.stream(PendingInviteSortField.values())
                .map(PendingInviteSortField::toSortOption)
                .toList();
    }
}


