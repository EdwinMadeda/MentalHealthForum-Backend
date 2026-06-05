package com.mentalhealthforum.mentalhealthforum_backend.config;


import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
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

@Component
public class AccessAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final UserModerationService userModerationService;

    public AccessAuthorizationManager(UserModerationService userModerationService) {
        this.userModerationService = userModerationService;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {

        String path = context.getExchange().getRequest().getPath().value();

        // Skip checks for public paths
        if(isPublicPath(path)) {
            return authentication.map(auth -> new AuthorizationDecision(true))
                    .defaultIfEmpty(new AuthorizationDecision(true));
        }

        return authentication
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuthenticationToken -> {
                    UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());// Get user ID from token

                    // check ban first (most severe)
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

                                            // Proceed with existing onboarding/role checks
                                            return checkOnboardingAndRoles(jwtAuthenticationToken, path);
                                        });
                            });

                })
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private Boolean isPublicPath(String path){
        String[] publicPaths = {
                "/api/auth",
                "/api/users/register",
                "/api/users/reset-password",
                "/api/users/verify",
                "/api/timezones",
                "/swagger-ui",
                "/v3/api-docs",
                "/actuator"
        };

        for(String publicPath: publicPaths){
            if(path.startsWith(publicPath)){
                return true;
            }
        }
        return false;
    }

    private Mono<AuthorizationDecision> checkOnboardingAndRoles(JwtAuthenticationToken jwtAuthenticationToken, String path) {

        boolean isOnboarding = hasRole(jwtAuthenticationToken, "ROLE_ONBOARDING");

        // If onboarding, check the path
        if(isOnboarding){
            // Allow them to update their own profile to satisfy requirements
            if(path.startsWith("/api/users") ||
                    path.startsWith("/api/auth") ||
                    path.startsWith("/api/onboarding")){
                return Mono.just(new AuthorizationDecision(true));
            }

            // Block everything else
            return Mono.just(new AuthorizationDecision(false));
        }

        // For admin paths, check ADMIN role
        if(path.startsWith("/api/admin")){
            boolean isAdmin = hasRole(jwtAuthenticationToken, "ROLE_ADMIN");
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
    private boolean hasRole(JwtAuthenticationToken jwtAuthenticationToken, String role){
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
