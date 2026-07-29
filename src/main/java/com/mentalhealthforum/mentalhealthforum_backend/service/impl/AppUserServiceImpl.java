package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.config.KeycloakProperties;
import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.onboarding.OnboardingPolicy;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UpdateUserProfileRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserInfoDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.InternalRole;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.enums.VerificationType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.AppUserSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.*;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AdminInvitationRepository;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.UserConnectRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.VerificationTokenRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AdminInvitationService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserResponseMapper;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AppUserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.KeycloakAdminManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.mentalhealthforum.mentalhealthforum_backend.utils.ChangeUtils.*;

@Service
public class AppUserServiceImpl implements AppUserService {

    private static final Logger log = LoggerFactory.getLogger(AppUserServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final KeycloakAdminManager adminManager;
    private final NovuServiceImpl novuServiceImpl;
    private final UserResponseMapper userResponseMapper;
    private final UserConnectRepository userConnectRepository;
    private final AdminInvitationService adminInvitationService;
    private final AdminInvitationRepository adminInvitationRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final WebClient webClient;
    private final String userInfoUri;

    private record KeycloakExtraDetails(Set<String> roles, Set<String> groups) {}

    public AppUserServiceImpl(
            AppUserRepository appUserRepository,
            WebClient.Builder webClientBuilder,
            KeycloakProperties keycloakProperties,
            KeycloakAdminManager adminManager,
            NovuServiceImpl novuServiceImpl,
            UserResponseMapper userResponseMapper,
            UserConnectRepository userConnectRepository,
            AdminInvitationService adminInvitationService,
            AdminInvitationRepository adminInvitationRepository,
            VerificationTokenRepository verificationTokenRepository) {
        this.appUserRepository = appUserRepository;
        this.adminManager = adminManager;
        this.novuServiceImpl = novuServiceImpl;
        this.userResponseMapper = userResponseMapper;
        this.userConnectRepository = userConnectRepository;
        this.adminInvitationService = adminInvitationService;
        this.adminInvitationRepository = adminInvitationRepository;
        this.verificationTokenRepository = verificationTokenRepository;

        String authServerUrl = keycloakProperties.getAuthServerUrl();
        String realm = keycloakProperties.getRealm();
        this.userInfoUri = String.format("/realms/%s/protocol/openid-connect/userinfo", realm);
        this.webClient = webClientBuilder.baseUrl(authServerUrl).build();
    }

    @Override
    public Mono<UserResponse> syncUserViaAdminClient(KeycloakUserDto keycloakUserDto, ViewerContext viewerContext) {
        AppUserEntity userDetails = new AppUserEntity(
                keycloakUserDto.userId(),
                keycloakUserDto.email(),
                keycloakUserDto.username(),
                keycloakUserDto.firstName(),
                keycloakUserDto.lastName());

        return fetchKeycloakExtraDetails(keycloakUserDto.userId())
                .flatMap(details -> {
                    userDetails.setIsEnabled(keycloakUserDto.enabled());
                    userDetails.setDateJoined(keycloakUserDto.getCreatedInstant());
                    userDetails.setLastSyncedAt(Instant.now());
                    userDetails.setRoles(details.roles());
                    userDetails.setGroups(details.groups());

                    return appUserRepository.findAppUserByKeycloakId(userDetails.getKeycloakId().toString())
                            .flatMap(existingUser -> {
                                boolean localNeedsUpdate = false;
                                localNeedsUpdate |= setIfChanged(userDetails.getEmail(), existingUser.getEmail(), existingUser::setEmail);
                                localNeedsUpdate |= setIfChanged(userDetails.getUsername(), existingUser.getUsername(), existingUser::setUsername);
                                localNeedsUpdate |= setIfChanged(userDetails.getFirstName(), existingUser.getFirstName(), existingUser::setFirstName);
                                localNeedsUpdate |= setIfChanged(userDetails.getLastName(), existingUser.getLastName(), existingUser::setLastName);
                                localNeedsUpdate |= setIfChanged(userDetails.getIsEnabled(), existingUser.getIsEnabled(), existingUser::setIsEnabled);
                                localNeedsUpdate |= setIfChanged(userDetails.getRoles(), existingUser.getRoles(), existingUser::setRoles);
                                localNeedsUpdate |= setIfChanged(userDetails.getGroups(), existingUser.getGroups(), existingUser::setGroups);

                                return localNeedsUpdate ? appUserRepository.save(existingUser) : Mono.just(existingUser);
                            })
                            .switchIfEmpty(Mono.defer(() ->
                                    isReadyForSyncing(keycloakUserDto)
                                            .flatMap(ready -> {
                                                if (!ready) {
                                                    log.debug("User {} not ready for sync", keycloakUserDto.userId());
                                                    return Mono.empty();
                                                }
                                                log.info("User {} transitioning to AppUserEntity", keycloakUserDto.userId());
                                                return adminInvitationService.completeInvitation(UUID.fromString(keycloakUserDto.userId()))
                                                        .then(appUserRepository.save(userDetails));
                                            })
                            ));
                })
                .flatMap(appUser -> {
                    if (appUser.getId() != null) {
                        novuServiceImpl.upsertSubscriber(appUser)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnError(e -> log.error("Novu sync failed for user {}", appUser.getKeycloakId()))
                                .subscribe();
                    }
                    return Mono.just(appUser);
                })
                .flatMap(appUser -> evaluateOnboardingPolicyCompliance(appUser, viewerContext).thenReturn(appUser))
                .flatMap(this::enrichWithPendingEmail)
                .flatMap(appUser -> enrichSingleUserWithConnectionStatus(appUser, viewerContext));
    }

    @Deprecated
    public Mono<UserResponse> syncUserViaAPI(String accessToken) {
        return getKeycloakUserInfo(accessToken)
                .flatMap(this::findOrCreateUser)
                .map(userResponseMapper::toSelfResponse)
                .onErrorMap(e -> {
                    log.error("Error syncing with Keycloak: {}", e.getMessage(), e);
                    return new KeycloakSyncException("An error occurred while syncing user data.", e);
                });
    }

    private Mono<UserInfoDto> getKeycloakUserInfo(String accessToken) {
        return webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.warn("Keycloak Profile Sync Failed ({}): {}", response.statusCode(), body))
                                .flatMap(body -> Mono.error(new KeycloakSyncException("Profile Sync failed. " + body)))
                )
                .bodyToMono(UserInfoDto.class)
                .doOnNext(userInfo -> log.info("Keycloak user info received: {}", userInfo))
                .doOnError(e -> log.error("Error fetching keycloak user info: {}", e.getMessage(), e));
    }

    private Mono<AppUserEntity> findOrCreateUser(UserInfoDto userInfoDto) {
        return appUserRepository.findAppUserByKeycloakId(userInfoDto.keycloakId())
                .flatMap(existingUser -> {
                    existingUser.setEmail(userInfoDto.email());
                    existingUser.setUsername(userInfoDto.preferredUsername());
                    existingUser.setFirstName(userInfoDto.givenName());
                    existingUser.setLastName(userInfoDto.familyName());
                    return appUserRepository.save(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    AppUserEntity newAppUser = new AppUserEntity(
                            userInfoDto.keycloakId(),
                            userInfoDto.email(),
                            userInfoDto.preferredUsername(),
                            userInfoDto.givenName(),
                            userInfoDto.familyName()
                    );
                    newAppUser.setBio(generateDefaultBio(userInfoDto.givenName(), userInfoDto.familyName()));
                    return appUserRepository.save(newAppUser);
                }));
    }

    @Override
    public Mono<UserResponse> getAppUserWithContext(String userId, ViewerContext viewerContext) {
        return appUserRepository.findAppUserByKeycloakId(userId)
                .switchIfEmpty(Mono.error(new UserDoesNotExistException()))
                .flatMap(appUser -> {
                    if (viewerContext.isAdmin()) {
                        return Mono.fromCallable(() -> adminManager.getUserRealmRoles(appUser.getKeycloakId().toString()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(HashSet::new)
                                .doOnNext(appUser::setRoles)
                                .thenReturn(appUser);
                    }
                    return Mono.just(appUser);
                })
                .flatMap(this::enrichWithPendingEmail)
                .flatMap(appUser -> enrichSingleUserWithConnectionStatus(appUser, viewerContext));
    }

    @Override
    public Mono<PaginatedResponse<UserResponse>> getActiveAppUsersWithContext(
            int page, int size, boolean currentUserFirst,
            Boolean isConnected,
            String role,
            String[] groups,
            String search,
            String sortBy,
            String sortDirection,
            ViewerContext viewerContext) {
        return executeGetAppUsersQuery(page, size, currentUserFirst, true, isConnected, role, groups, search, sortBy, sortDirection, viewerContext);
    }

    @Override
    public Mono<PaginatedResponse<UserResponse>> getAllAppUsersWithContext(
            int page, int size, boolean currentUserFirst,
            Boolean isActive,
            Boolean isConnected,
            String role,
            String[] groups,
            String search,
            String sortBy,
            String sortDirection,
            ViewerContext viewerContext) {
        return ModerationAction.USER_VIEW_INACTIVE.checkPermission(viewerContext)
                .then(executeGetAppUsersQuery(page, size, currentUserFirst, isActive, isConnected, role, groups, search, sortBy, sortDirection, viewerContext));
    }

    private AppUserSortField validateAndNormalizeSortBy(String sortBy) {
        return AppUserSortField.fromString(sortBy);
    }

    @Override
    public Mono<Void> updateLocalEmail(String userId, String newEmail) {
        return appUserRepository.findAppUserByKeycloakId(userId)
                .switchIfEmpty(Mono.error(new UserDoesNotExistException()))
                .flatMap(appUser -> {
                    boolean localEmailNeedsUpdate = setIfChangedStrict(newEmail, appUser.getEmail(), appUser::setEmail);
                    return localEmailNeedsUpdate ? appUserRepository.save(appUser) : Mono.just(appUser);
                })
                .doOnSuccess(user -> log.info("Successfully synced local email for user: {}", userId))
                .then();
    }

    @Override
    public Mono<UserResponse> updateLocalProfile(String userId, ViewerContext viewerContext, UpdateUserProfileRequest updateUserProfileRequest) {
        return appUserRepository.findAppUserByKeycloakId(userId)
                .switchIfEmpty(Mono.error(new UserDoesNotExistException()))
                .flatMap(appUser -> {
                    if (!viewerContext.getUserId().equals(userId)) {
                        return Mono.error(new InsufficientPermissionException("Forbidden: Cannot update another user's profile."));
                    }

                    boolean localNeedsUpdate = false;
                    localNeedsUpdate |= setIfChangedStrict(updateUserProfileRequest.firstName(), appUser.getFirstName(), appUser::setFirstName);
                    localNeedsUpdate |= setIfChangedStrict(updateUserProfileRequest.lastName(), appUser.getLastName(), appUser::setLastName);
                    localNeedsUpdate |= setIfChangedAllowNull(updateUserProfileRequest.displayName(), appUser.displayName(), appUser::setDisplayName);
                    localNeedsUpdate |= setIfChangedAllowNull(updateUserProfileRequest.bio(), appUser.bio(), appUser::setBio);
                    localNeedsUpdate |= setIfChangedAllowNull(updateUserProfileRequest.avatarUrl(), appUser.getAvatarUrl(), appUser::setAvatarUrl);
                    localNeedsUpdate |= setIfChangedAllowNull(updateUserProfileRequest.timezone(), appUser.timezone(), appUser::setTimezone);
                    localNeedsUpdate |= setIfChangedAllowNull(updateUserProfileRequest.profileVisibility(), appUser.getProfileVisibility(), appUser::setProfileVisibility);

                    return localNeedsUpdate ? appUserRepository.save(appUser) : Mono.just(appUser);
                })
                .flatMap(savedUser ->
                        novuServiceImpl.upsertSubscriber(savedUser)
                                .then(evaluateOnboardingPolicyCompliance(savedUser, viewerContext))
                                .thenReturn(savedUser))
                .flatMap(this::enrichWithPendingEmail)
                .flatMap(appUser -> enrichSingleUserWithConnectionStatus(appUser, viewerContext));
    }

    @Override
    public Mono<UserDetails> getUserDetails(UUID userId) {
        return appUserRepository.findAppUserByKeycloakId(userId.toString())
                .map(AppUserEntity::toUserDetails)
                .defaultIfEmpty(AppUserEntity.defaultUser());
    }

    @Deprecated
    private String generateDefaultBio(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return String.format("Hi, I'm %s %s. I'm here to connect, learn and grow. Let's support each other!", firstName, lastName);
        }
        return "Hi, I'm here to connect, learn and grow. Let's support each other!";
    }

    private Mono<Boolean> isReadyForSyncing(KeycloakUserDto keycloakUserDto) {
        return adminInvitationRepository.findByKeycloakId(UUID.fromString(keycloakUserDto.userId()))
                .map(adminInvitation -> keycloakUserDto.emailVerified()
                        && adminInvitation.getCurrentStage() == OnboardingStage.AWAITING_PROFILE_COMPLETION)
                .defaultIfEmpty(keycloakUserDto.emailVerified());
    }

    private Mono<UpdateUserProfileRequest> validateOnboardingPolicy(UpdateUserProfileRequest updateUserProfileRequest, ViewerContext viewerContext) {
        OnboardingPolicy.Result result = viewerContext.checkOnboardingPolicy(updateUserProfileRequest);
        if (!result.isSatisfied()) {
            return Mono.error(new OnboardingPolicyViolationException(result.violations()));
        }
        return Mono.just(updateUserProfileRequest);
    }

    private Mono<Void> evaluateOnboardingPolicyCompliance(AppUserEntity appUser, ViewerContext viewerContext) {
        return Mono.fromRunnable(() -> {
            String userId = String.valueOf(appUser.getKeycloakId());
            if (viewerContext.checkOnboardingPolicy(appUser).isSatisfied()) {
                adminManager.removeInternalRole(userId, InternalRole.ONBOARDING);
            } else {
                adminManager.assignInternalRole(userId, InternalRole.ONBOARDING);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<KeycloakExtraDetails> fetchKeycloakExtraDetails(String userId) {
        return Mono.fromCallable(() -> {
            List<String> roles = adminManager.getUserRealmRolesFromGroups(userId);
            List<String> groups = adminManager.getUserGroups(userId);
            return new KeycloakExtraDetails(new HashSet<>(roles), new HashSet<>(groups));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AppUserEntity> enrichWithPendingEmail(AppUserEntity appUser) {
        if (appUser.getEmail() == null) {
            return Mono.just(appUser);
        }
        return verificationTokenRepository.findByEmailAndType(appUser.getEmail(), VerificationType.APP_USER)
                .map(verificationToken -> {
                    appUser.setPendingEmail(verificationToken.getNewValue());
                    return appUser;
                })
                .defaultIfEmpty(appUser);
    }

    private Mono<PaginatedResponse<UserResponse>> executeGetAppUsersQuery(
            int page, int size, boolean currentUserFirst,
            Boolean isActive,
            Boolean isConnected,
            String role,
            String[] groups,
            String search,
            String sortBy,
            String sortDirection,
            ViewerContext viewerContext) {

        if (page < 0 || size <= 0) {
            throw new InvalidPaginationException();
        }

        int offset = page * size;
        UUID currentUserId = null;
        boolean isAdmin = false;
        boolean isModeratorOrAdmin = false;

        if (viewerContext != null && viewerContext.getUserId() != null) {
            try {
                currentUserId = UUID.fromString(viewerContext.getUserId());
                isAdmin = viewerContext.isAdmin();
                isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
            } catch (IllegalArgumentException e) {
                log.error("Failed to parse viewer keycloak UUID: {}", viewerContext.getUserId());
            }
        }

        String[] effectiveGroups = (groups == null || groups.length == 0) ? null : groups;
        String effectiveSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        AppUserSortField sortByField = validateAndNormalizeSortBy(sortBy);
        String normalizedDirection = sortByField.determineSortDirection(sortDirection);

        boolean applyCurrentUserFirst = currentUserFirst && page == 0;

        Flux<AppUserEntity> appUsersFlux = appUserRepository.findAllPaginated(
                isActive, role, effectiveGroups,
                currentUserId, applyCurrentUserFirst,
                isAdmin, isModeratorOrAdmin,
                isConnected, effectiveSearch,
                sortByField.getValue(), normalizedDirection,
                size, offset);

        Mono<Long> totalCount = appUserRepository.countAll(
                isActive, role, effectiveGroups,
                currentUserId,
                isAdmin, isModeratorOrAdmin,
                isConnected, effectiveSearch);

        UUID finalCurrentUserId = currentUserId;
        return Mono.zip(appUsersFlux.collectList(), totalCount)
                .flatMap(tuple -> {
                    List<AppUserEntity> appUsers = tuple.getT1();
                    long total = tuple.getT2();

                    if (appUsers.isEmpty()) {
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, total));
                    }

                    return enrichAppUsersWithConnectionStatus(appUsers, finalCurrentUserId, viewerContext)
                            .map(content -> {
                                FilterMetadata<Object> filters = FilterMetadata.builder()
                                        .sortOptions(getUserSortOptions())
                                        .build();
                                return new PaginatedResponse<>(content, page, size, total, filters);
                            });
                });
    }

    private Mono<UserResponse> enrichSingleUserWithConnectionStatus(AppUserEntity targetUser, ViewerContext viewerContext) {
        Mono<Boolean> connectionMono;
        if (viewerContext == null || viewerContext.getUserId() == null) {
            connectionMono = Mono.just(false);
        } else {
            UUID viewerId = UUID.fromString(viewerContext.getUserId());
            UUID targetId = targetUser.getKeycloakId();
            if (viewerId.equals(targetId)) {
                connectionMono = Mono.just(false);
            } else {
                connectionMono = userConnectRepository.areConnected(viewerId, targetId).defaultIfEmpty(false);
            }
        }

        return connectionMono.map(isConnected -> {
            targetUser.setIsConnected(isConnected);
            return userResponseMapper.mapUserBasedOnContext(targetUser, viewerContext);
        });
    }

    private Mono<List<UserResponse>> enrichAppUsersWithConnectionStatus(
            List<AppUserEntity> appUsers,
            UUID currentUserId,
            ViewerContext viewerContext) {

        if (currentUserId == null) {
            return Mono.just(appUsers.stream()
                    .map(appUser -> {
                        appUser.setIsConnected(false);
                        return userResponseMapper.mapUserBasedOnContext(appUser, viewerContext);
                    })
                    .collect(Collectors.toList()));
        }

        List<UUID> userIds = appUsers.stream()
                .map(AppUserEntity::getKeycloakId)
                .toList();

        return userConnectRepository.findConnectedUserIds(currentUserId, userIds)
                .collectList()
                .map(connectedIds -> {
                    Set<UUID> connectedSet = new HashSet<>(connectedIds);
                    return appUsers.stream()
                            .map(appUser -> {
                                appUser.setIsConnected(connectedSet.contains(appUser.getKeycloakId()));
                                return userResponseMapper.mapUserBasedOnContext(appUser, viewerContext);
                            })
                            .collect(Collectors.toList());
                });
    }

    private List<SortOption> getUserSortOptions() {
        return Arrays.stream(AppUserSortField.values())
                .map(AppUserSortField::toSortOption)
                .toList();
    }
}