package com.mentalhealthforum.mentalhealthforum_backend.config;


import com.mentalhealthforum.mentalhealthforum_backend.contants.SecurityConstants;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserModerationService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Asks "Should this request be allowed?"
 * */
@Component
public class AccessAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final UserModerationService userModerationService;
    private final AppUserRepository appUserRepository;

    public AccessAuthorizationManager(UserModerationService userModerationService, AppUserRepository appUserRepository) {
        this.userModerationService = userModerationService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {

        String path = context.getExchange().getRequest().getPath().value();

        // Skip checks for public paths
        if(isPublicPath(path)) {
            return authentication.map(auth -> new AuthorizationDecision(true))
                    .defaultIfEmpty(new AuthorizationDecision(true));
        }

        // Bypass account status check for reactivation endpoint
        if(isReactivationPath(path)){
            return authentication.map(auth -> new AuthorizationDecision(true))
                    .defaultIfEmpty(new AuthorizationDecision(false));
        }

        return authentication
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuthenticationToken -> {
                    UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());// Get user ID from token

                    // Check account status first
                    return appUserRepository.findAppUserByKeycloakId(userId.toString())
                            .flatMap(appUser -> {
                                if(appUser.isPendingDeletion() || appUser.isPurged()){
                                    return Mono.just(new AuthorizationDecision(false));
                                }

                                // check ban next (most severe)
                                return userModerationService.isUserBanned(userId)
                                        .flatMap(isBanned -> {
                                            if(isBanned){
                                                return Mono.just(new AuthorizationDecision(false));
                                            }

                                            // Check suspension
                                            return userModerationService.isUserSuspended(userId)
                                                    .flatMap(isRestricted -> {
                                                        if(isRestricted){
                                                            return Mono.just(new AuthorizationDecision(false));
                                                        }

                                                        // Proceed with accountStatus/onboarding/role checks
                                                        return checkMfaOnboardingAndRoles(appUser, jwtAuthenticationToken, path);

                                                    });
                                        });

                            });

                })
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private Boolean isPublicPath(String path){
        return SecurityConstants.PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Boolean isReactivationPath(String  path){
        return path.matches(SecurityConstants.REACTIVATION_PATH_REGEX);
    }

    private Mono<AuthorizationDecision> checkMfaOnboardingAndRoles( AppUserEntity appUser, JwtAuthenticationToken jwtAuthenticationToken, String path) {

        //boolean isOnboarding = hasRole(jwtAuthenticationToken, "ROLE_ONBOARDING");
        boolean isAdmin = hasRole(jwtAuthenticationToken, "ROLE_ADMIN");

        if(path.startsWith("/api/admin")){
            // Allow MFA setup endpoints without MFA (so admin can enable it)
            if(isMfaSetupPath(path)){
                return Mono.just(new AuthorizationDecision(isAdmin));
            }

            // MFA must be enabled for all other admin endpoints
            if(isAdmin && !appUser.isMfaEnabled()){
                return Mono.just(new AuthorizationDecision(false));
            }
        }

        // If onboarding, check the path
        if(appUser.isOnboarding()){
            // Allow self-profile (to satisfy requirements) operations (get, update, reactivate)
            if(isOnboardingPath(path)) {
                return Mono.just(new AuthorizationDecision(true));
            }

            // Allow onboarding-specific endpoints
            if(path.startsWith("/api/onboarding")){
                return Mono.just(new AuthorizationDecision(true));
            }

            // Allow auth endpoints (login, refresh, logout)
            if(path.startsWith("/api/auth")){
                return Mono.just(new AuthorizationDecision(true));
            }

            // Block everything else (including /api/users)
            return Mono.just(new AuthorizationDecision(false));
        }

        // For admin paths, check ADMIN role
        if(path.startsWith("/api/admin")){
            return Mono.just(new AuthorizationDecision(isAdmin));
        }

        if(path.startsWith("/api/moderator")){
            boolean isModeratorOrAdmin = hasAnyRole(jwtAuthenticationToken,  "ROLE_ADMIN", "ROLE_MODERATOR");
            return Mono.just(new AuthorizationDecision(isModeratorOrAdmin));
        }

        if(path.startsWith("/api/peer")){
            boolean isPeerOrHigher = hasAnyRole(jwtAuthenticationToken, "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_PEER_SUPPORTER");
            return Mono.just(new AuthorizationDecision(isPeerOrHigher));
        }

        // If not onboarding Let subsequent checks (Method Security) handle it.
        // For all other paths, allow
        return Mono.just(new AuthorizationDecision(true));

    }

    // Helper methods
    private boolean isMfaSetupPath(String path){
        return path.startsWith("/api/admin/mfa/setup") ||
               path.startsWith("/api/admin/mfa/confirm");
    }

    private boolean isOnboardingPath(String path){
        return path.matches("/api/users/[a-f0-9-]+") ||  // GET /api/users/{uuid}
                path.matches("/api/users/profile") ||      // PATCH/DELETE /api/users/profile
                path.matches("/api/users/reactivate") ||   // POST /api/users/reactivate
                path.matches("/api/users/reset-password"); // POST /api/users/reset-password
    }

    private boolean hasRole(JwtAuthenticationToken jwtAuthenticationToken, String role) {
        return jwtAuthenticationToken.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals(role));
    }

    private boolean hasAnyRole(JwtAuthenticationToken jwtAuthenticationToken, String ...roles){
        Set<String> roleSet = Set.of(roles);
        return jwtAuthenticationToken.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        roleSet.contains( grantedAuthority.getAuthority()));
    }




}
