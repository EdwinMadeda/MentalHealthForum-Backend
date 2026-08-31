package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentalhealthforum.mentalhealthforum_backend.config.KeycloakProperties;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.AuthResult;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.JwtResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.LoginRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.AuthenticationFailedException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidTokenException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.UserActionRequiredException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AdminInvitationRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.*;
import com.mentalhealthforum.mentalhealthforum_backend.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_EXPIRY_SECONDS;
import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_LENGTH;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.MaskEmailUtils.maskEmail;

/**
 * Implementation of the AuthService using WebClient to interact with Keycloak
 * for manual authentication (ROPC Grant) and token refreshing.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    // Constant for the specific Keycloak error that indicates required actions are pending.
    private static final String KEYCLOAK_REQUIRED_ACTION_ERROR = "Account is not fully set up";

    private final ObjectMapper objectMapper = new ObjectMapper();


    private final AdminInvitationRepository adminInvitationRepository;
    private final AppUserService appUserService;
    private final UserActivityService userActivityService;
    private final MfaService mfaService;
    private final AppUserRepository appUserRepository;
    private final KeycloakAdminManager adminManager;
    private final KeycloakUserDtoMapper keycloakUserDtoMapper;
    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final JwtUtils jwtUtils;
    private final MfaStateCache mfaStateCache;

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String logoutUri;

    public AuthServiceImpl(
            AdminInvitationRepository adminInvitationRepository,
            AppUserService appUserService,
            UserActivityService userActivityService,
            MfaService mfaService, AppUserRepository appUserRepository,
            KeycloakAdminManager adminManager,
            KeycloakUserDtoMapper keycloakUserDtoMapper,
            JwtClaimsExtractor jwtClaimsExtractor,
            JwtUtils jwtUtils, MfaStateCache mfaStateCache,
            WebClient.Builder webClientBuilder,
            KeycloakProperties properties) {
        this.adminInvitationRepository = adminInvitationRepository;
        this.appUserService = appUserService;
        this.userActivityService = userActivityService;
        this.mfaService = mfaService;
        this.appUserRepository = appUserRepository;
        this.adminManager = adminManager;
        this.keycloakUserDtoMapper = keycloakUserDtoMapper;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.jwtUtils = jwtUtils;
        this.mfaStateCache = mfaStateCache;

        String authServerUrl = properties.getAuthServerUrl();
        String realm = properties.getRealm();

        this.clientId = properties.getResource();
        this.clientSecret = properties.getCredentials().getSecret();

        // Base URI for token and refresh endpoints
        String tokenUri = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        // URI for the logout/revocation endpoint
        this.logoutUri = String.format("%s/realms/%s/protocol/openid-connect/logout", authServerUrl, realm);

        // WebClient MUST be built with the specific token URI
        this.webClient = webClientBuilder.baseUrl(tokenUri).build();
    }

    /**
     * Authenticates a user against Keycloak using ROPC (Resource Owner Password Credentials) Grant.
     *
     * @param request LoginRequest containing username and password.
     * @return Mono<JwtResponse> containing the access and refresh tokens.
     */
    @Override
    public Mono<AuthResult> authenticate(LoginRequest request, String ipAddress, String userAgent){
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // 1. OAuth2 Grant Type and Client Credentials
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        // 2. AppUserEntity Credentials
        formData.add("username", request.username());
        formData.add("password", request.password());

        // 3. Adding 'openid' scope for userinfo endpoint access
        formData.add("scope", "openid");

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        // Intercept only 4xx errors (Invalid Credentials, etc.)
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(body ->  log.error("Keycloak Login Error Response (4xx): {}", body))
                                    .flatMap(body -> {
                                        try {

                                            Map<String, String> errorBody = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
                                            String errorDescription = errorBody.getOrDefault("error_description", "Authentication failed.");

                                            // Pass error description directly with minimal hints
                                            if(KEYCLOAK_REQUIRED_ACTION_ERROR.equals(errorDescription)){
                                                return Mono.error(new UserActionRequiredException(
                                                        errorDescription + ". Please check your inbox for verification"
                                                ));
                                            }

                                            return Mono.error(new AuthenticationFailedException(errorDescription));

                                        } catch (JsonProcessingException e) {
                                            log.error("Could not parse Keycloak error response as JSON: {}", body, e);
                                            return Mono.error(new AuthenticationFailedException(
                                                    "Authentication failed."
                                            ));
                                        }
                                    });
                        })
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Keycloak Server Error (5xx): {}", body))
                                    .then(Mono.error(new ApiException(
                                            "Authentication service is temporarily unavailable.",
                                            ErrorCode.AUTHENTICATION_SERVICE_ERROR
                                    )));
                        })
                .bodyToMono(JwtResponse.class)
                .flatMap(jwtResponse -> {
                    // Extract keycloak UUID (sub) from the token
                    Jwt jwt = jwtUtils.createJwtFromToken(jwtResponse.accessToken());
                    UUID keycloakId = UUID.fromString(jwt.getSubject());

                    // Check if MFA is enabled for user
                    return appUserRepository.findAppUserByKeycloakId(keycloakId.toString())
                            .flatMap(appUser -> {
                                // MFA is enabled — challenge the admin
                                if(appUser.isMfaEnabled()){
                                    // Don't issue tokens yet — hold them for MFA verification
                                    return handleMfaChallenge(appUser, jwtResponse, ipAddress, userAgent);
                                }
                                return proceedWithLogin(jwtResponse, keycloakId);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // User doesn't exist in DB yet - proceed with normal login
                                log.info("User not yet synced, proceed with login : {}", keycloakId);
                                return proceedWithLogin(jwtResponse, keycloakId);
                            }));

                })
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Cannot connect to authentication service: {}", e.getMessage());
                    return Mono.error(new ApiException(
                            "Authentication service is temporarily unavailable. Please try again later.",
                            ErrorCode.AUTHENTICATION_SERVICE_ERROR,
                            e
                    ));
                })
                .onErrorResume(io.netty.channel.ConnectTimeoutException.class, e -> {
                    log.error("Connection timeout to authentication service: {}", e.getMessage());
                    return Mono.error(new ApiException(
                            "Authentication service connection timeout. Please try again.",
                            ErrorCode.AUTHENTICATION_SERVICE_ERROR,
                            e
                    ));
                });
    }

    /**
     * Exchanges a refresh token for a new set of access and refresh tokens using the Refresh Token Grant.
     * @param refreshToken The expired refresh token.
     * @return Mono<JwtResponse> containing the new tokens.
     */
    @Override
    public Mono<JwtResponse> refreshTokens(String refreshToken){
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // 1. Correct Grant Type and Client Credentials
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        // 2. The token to be refreshed
        formData.add("refresh_token", refreshToken);

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        // Intercept only 4xx errors (Invalid Token)
                        HttpStatusCode::is4xxClientError,
                        response ->  {
                            return response.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Keycloak Refresh Failed (4xx) : {}", body))
                                    .flatMap(body -> {
                                        return Mono.error(new AuthenticationFailedException(
                                                "Token refresh failed. The refresh token is invalid or expired."
                                        ));
                                    });
                        })
                .bodyToMono(JwtResponse.class)
                .flatMap(jwtResponse -> {
                    Jwt jwt = jwtUtils.createJwtFromToken(jwtResponse.accessToken());
                    UUID keycloakId = UUID.fromString(jwt.getSubject());


                    // Critical path: Database updates
                    Mono<Void> activityUpdates = userActivityService.trackActivity(keycloakId);

                    // Fire-and-forget: Sync in the background
                    syncUserAfterAuth(jwtResponse.accessToken())
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(v -> log.debug("On refresh: Background sync completed for user {}", keycloakId))
                            .doOnError(e -> log.error("On refresh: Background sync failed for user {}", keycloakId, e))
                            .subscribe();

                    // Return immediately after activity updates
                    return activityUpdates.thenReturn(jwtResponse);

                });
    }

    @Override
    public Mono<Void> logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        // Build a new WebClient to target the absolute logout URI
        return WebClient.builder().build().post()
                .uri(logoutUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> {
                            return response.bodyToMono(String.class)
                                    .doOnNext(body -> log.warn("Keycloak Logout Failed ({}): {}", response.statusCode(), body))
                                    .flatMap(body -> {
                                        return Mono.error(new ApiException(
                                                "Logout failed. Please try again.",
                                                ErrorCode.AUTHENTICATION_SERVICE_ERROR,
                                                null
                                        ));
                                    });
                        })
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Unexpected error during Keycloak logout: {}", e.getMessage()));
    }

    @Override
    public Mono<JwtResponse> issueFullTokens(String stateToken){

        return mfaStateCache.getState(stateToken)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired MFA state")))
                .flatMap(mfaState -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

                    // 1. Correct Grant Type and Client Credentials
                    formData.add("grant_type", "refresh_token");
                    formData.add("client_id", clientId);
                    formData.add("client_secret", clientSecret);

                    // 2. The token to be refreshed
                    formData.add("refresh_token", mfaState.refreshToken());

                    return webClient.post()
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .onStatus(
                                    // Intercept only 4xx errors (Invalid Token)
                                    HttpStatusCode::is4xxClientError,
                                    response ->  {
                                        return response.bodyToMono(String.class)
                                                .doOnNext(body -> log.error("Keycloak Issue Full Token Failed (4xx) : {}", body))
                                                .flatMap(body -> {
                                                    return Mono.error(new AuthenticationFailedException(
                                                            "Full Token issue failed. The refresh token is invalid or expired."
                                                    ));
                                                });
                                    })
                            .bodyToMono(JwtResponse.class)
                            .flatMap(jwtResponse -> {
                                Jwt jwt = jwtUtils.createJwtFromToken(jwtResponse.accessToken());
                                UUID keycloakId = UUID.fromString(jwt.getSubject());

                                // Critical path: Database updates
                                Mono<Void> activityUpdates = userActivityService.trackActivity(keycloakId);

                                // Fire-and-forget: Sync in the background
                                syncUserAfterAuth(jwtResponse.accessToken())
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnSuccess(v -> log.debug("On Full Token Reissue: Background sync completed for user {}", keycloakId))
                                        .doOnError(e -> log.error("On Full Token Reissue: Background sync failed for user {}", keycloakId, e))
                                        .subscribe();

                                // Return immediately after activity updates
                                return activityUpdates.thenReturn(jwtResponse);

                            });
                });

    }


    // ==================== PRIVATE HELPERS ====================

    /**
     * Handles MFA challenge flow for users with MFA enabled.
     * Generates a state token, sends OTP, and returns MFA challenge response.
     */
    private Mono<AuthResult> handleMfaChallenge(AppUserEntity appUser, JwtResponse jwtResponse, String ipAddress, String userAgent) {
        // Don't issue tokens yet — we'll issue them after MFA verification
        // The tokens are held temporarily in memory (or we could store them in the state)

        return mfaService.initiateMfaChallenge(appUser, ipAddress, userAgent, jwtResponse.accessToken(), jwtResponse.refreshToken())
                .flatMap(mfaState -> {
                    // Return MFA challenge response instead of tokens
                    // The frontend should show the MFA input screen
                    return Mono.just(new AuthResult.MfaRequired(
                            mfaState.stateToken(),
                            maskEmail(appUser.getEmail()),
                            OTP_LENGTH,
                            OTP_EXPIRY_SECONDS
                    ));
                });

    }

    /**
     * Proceed with normal login flow (MFA not enabled)
     * */
    private Mono<AuthResult> proceedWithLogin(JwtResponse jwtResponse, UUID keycloakId) {
        // Existing login flow logic
        return  adminInvitationRepository.findByKeycloakId(keycloakId)
                .flatMap(adminInvitation -> {
                    boolean isRestrictedStage =
                            adminInvitation.getCurrentStage() == OnboardingStage.AWAITING_VERIFICATION ||
                                    adminInvitation.getCurrentStage() == OnboardingStage.AWAITING_PASSWORD_RESET;

                    // If the one-time pass was already invalidated, block entry
                    if(isRestrictedStage && Boolean.FALSE.equals(adminInvitation.getIsInitialLogin())){
                        log.warn("Access denied: One time pass for user {} has already been used", keycloakId);
//                                   log.warn("Access denied: Temporary credentials for user {} already used or expired.", keycloakId);
                        return Mono.error(
                                new AuthenticationFailedException("Your temporary password has expired. Please use 'Forgot Password' to set a new one.")
                        );
                    }
                    // First time? Invalidate the pass and let them through
                    return adminInvitationRepository.invalidateOneTimePass(keycloakId)
                            .thenReturn(jwtResponse);
                })
                // No Lobby record? They are a regular user, let them through
                .defaultIfEmpty(jwtResponse)
                .flatMap(response -> {

                    // Critical path: Database updates
                    Mono<Void> activityUpdates = userActivityService.recordLoginActivity(keycloakId);

                    // Fire-and-forget: Sync in the background
                    syncUserAfterAuth(jwtResponse.accessToken())
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(v -> log.debug("On Login: Background sync completed for user {}", keycloakId))
                            .doOnError(e -> log.error("On Login: Background sync failed for user {}", keycloakId, e))
                            .subscribe();

                    // Return immediately after activity updates
                    return activityUpdates.thenReturn(new AuthResult.Success(jwtResponse));

                });

    }

    private Mono<Void> syncUserAfterAuth(String accessToken){
        return Mono.fromCallable(()-> {
                Jwt jwt = jwtUtils.createJwtFromToken(accessToken);
                ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
                String userId = viewerContext.getUserId();

                KeycloakUserDto keycloakUserDto =  adminManager.findUserByUserId(userId)
                    .map(keycloakUserDtoMapper::mapToKeycloakUserDto)
                    .orElseThrow(()-> new RuntimeException("User not found in keycloak during auth sync"));

                return new SyncContext(viewerContext, keycloakUserDto);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(ctx ->
                  appUserService.syncUserViaAdminClient(ctx.keycloakUserDto, ctx.viewerContext)
                            .doOnSuccess(user -> log.debug("User {} synced successfully during auth flow.", ctx.viewerContext.getUserId()))
                            .doOnError(e -> log.error("Failed to sync user {} during auth flow: {}", ctx.viewerContext.getUserId(), e.getMessage()))
                            .then()
                )
                .onErrorResume(e -> {
                    log.error("Unexpected error during auth sync: {}", e.getMessage(), e);
                    return Mono.empty(); // Never break authentication flow
                });

    }

    /**
     * Simple holder for sync context
     */
    private record SyncContext(ViewerContext viewerContext, KeycloakUserDto keycloakUserDto){}


}