package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.contants.MfaConstants;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaPendingState;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.AuthenticationFailedException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_MAX_ATTEMPTS;

@Component
public class MfaStateCache {

    private static final Logger log = LoggerFactory.getLogger(MfaStateCache.class);
    private final Map<String, MfaPendingState> cache = new ConcurrentHashMap<>();

    public String createState(UUID userId, String email, String ipAddress, String userAgent,  String accessToken, String refreshToken){
        String stateToken = UUID.randomUUID().toString();
        MfaPendingState state = new MfaPendingState(
                stateToken,
                userId,
                email,
                accessToken,
                refreshToken,
                Instant.now().plus(MfaConstants.MFA_STATE_TOKEN_EXPIRY_DURATION_MINUTES),
                ipAddress,
                userAgent,
                0 // Start at 0 failures
        );
        cache.put(stateToken, state);
        return stateToken;
    }

    public Mono<MfaPendingState> getState(String stateToken){
        if(stateToken == null || stateToken.isBlank()){
            return Mono.empty();
        }

        MfaPendingState state = cache.get(stateToken);
        if(state == null){
            log.debug("MFA state not found: {}", stateToken);
            return Mono.empty();
        }

        if(state.isExpired()){
            cache.remove(stateToken);
            log.debug("MFA state expired: {}", stateToken);
            return Mono.empty();
        }

        if(state.isMaxedOut()){
            cache.remove(stateToken);
            log.debug("MFA state maxed out: {}", stateToken);
            return Mono.empty();
        }

        return Mono.just(state);
    }

    public void recordFailedAttempt(String stateToken){
        if(stateToken == null || stateToken.isBlank()){
            return;
        }

        cache.computeIfPresent(stateToken, (key, existingState) -> {
            MfaPendingState updated = existingState.incrementFailure();

            log.warn("MFA failed attempt {}/{}. User: {}, IP: {}",
                    updated.failedAttempts(),
                    OTP_MAX_ATTEMPTS,
                    existingState.email(),
                    existingState.ipAddress());

            if(updated.isMaxedOut()) {
                log.error("MFA LOCKOUT - User: {}, IP: {}, Email: {}",
                        existingState.userId(),
                        existingState.ipAddress(),
                        existingState.email()
                );

                return null; // Returning null inside computeIfPresent cleanly REMOVES the key from the map
            }
            return updated;
        });
    }

    public void invalidate(String stateToken){
        if(stateToken != null){
            cache.remove(stateToken);
            log.debug("MFA state invalidated: {}", stateToken);
        }
    }

    public void cleanup(){
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry ->
                entry.getValue().isExpired() || entry.getValue().isMaxedOut());
        int removed = initialSize - cache.size();
        if(removed > 0){
            log.debug("Cleaned up {} expired MFA states", removed);
        }
    }

}
