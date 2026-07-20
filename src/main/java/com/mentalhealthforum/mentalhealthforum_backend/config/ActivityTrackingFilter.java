package com.mentalhealthforum.mentalhealthforum_backend.config;

import com.mentalhealthforum.mentalhealthforum_backend.contants.SecurityConstants;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(3) // After authentication filter
public class ActivityTrackingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ActivityTrackingFilter.class);

    // Rate limit: Update at most once every 5 minutes
    private static final Duration UPDATE_THRESHOLD = Duration.ofMinutes(5);

    private static final List<String> PUBLIC_PATHS = Arrays.stream(SecurityConstants.AUTH_WHITELIST)
            .map(pattern -> pattern.replace("/**", "")) // Convert /** to prefix match
            .toList();

    private final AppUserRepository appUserRepository;
    private final JwtUtils jwtUtils;

    // In-memory cache to reduce DB reads
    private final ConcurrentHashMap<String, Instant> activityCache = new ConcurrentHashMap<>();

    public ActivityTrackingFilter(AppUserRepository appUserRepository, JwtUtils jwtUtils) {
        this.appUserRepository = appUserRepository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Skip public endpoints (mirrors SecurityConfig whitelist)
        if(isPublicEndpoint(exchange)){
          return chain.filter(exchange);
        }

        // For authenticated endpoints, Process requests first, then update activity asynchronously
        return chain.filter(exchange)
                .then(Mono.defer(() -> updateActivityIfNeeded(exchange)))
                .then();
    }

    private boolean isPublicEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> updateActivityIfNeeded(ServerWebExchange exchange) {
        String userId = extractUserId(exchange);
        if(userId == null){
            return Mono.empty();
        }

        // Check cache first – no DB read if we've updated recently
        Instant lastUpdate = activityCache.get(userId);
        if(lastUpdate != null && !shouldUpdate(lastUpdate)){
            // We updated within the threshold – skip
            return Mono.empty();
        }

        // Cache miss or expired – check the database
        try{
            UUID keycloakId = UUID.fromString(userId);

            return appUserRepository.findById(keycloakId)
                    .filter(appUser -> {
                        // If user doesn't exist in the DB, skip
                        if(appUser == null){
                            log.warn("User not found in app_users for activity tracking: {}", userId);
                            return false;
                        }

                        // Check last_active_updated_at from the database
                        Instant dbLastUpdate = appUser.getLastActiveUpdatedAt();
                        return shouldUpdate(dbLastUpdate);
                    })
                    .flatMap(appUser -> {
                        // Update both timestamps atomically
                        Instant now = Instant.now();
                        return appUserRepository.updateLastActive(keycloakId, now)
                                .doOnSuccess(updated -> {
                                    if(updated > 0){
                                        activityCache.put(userId, now);
                                        log.debug("Updated last_active_at for user {}", userId);
                                    }
                                })
                                .doOnError(e -> log.error("Failed to update last_active_at for {}: {}", userId, e.getMessage()))
                                .then();
                    })
                    .onErrorResume(e -> {
                        log.error("Activity tracking failed for user {}: {}", userId, e.getMessage());
                        return Mono.empty();
                    });
        } catch (IllegalArgumentException e){
            log.error("Invalid UUID format: {}", userId);
            return Mono.empty();
        }
    }

    /**
     * Checks if cooldown period has expired
     * Returns true if:
     * - lastUpdate is null (never updated)
     * - UPDATE_THRESHOLD or more minutes have passed since lastUpdate
     * */
    private boolean shouldUpdate(Instant lastUpdate) {
        if(lastUpdate == null){
            return true;
        }
        return Duration.between(lastUpdate, Instant.now())
                .compareTo(UPDATE_THRESHOLD) >= 0;
    }

    /**
     * Extracts the userId from the JWT Token in the Authorization header.
     * */
    private String extractUserId(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            return null;
        }

        String token = authHeader.substring(7);
        try{
            Jwt jwt = jwtUtils.createJwtFromToken(token);
            return jwt.getSubject();
        }
        catch (JwtException e){
            // Invalid token – just return null
            return null;
        }
    }
}
