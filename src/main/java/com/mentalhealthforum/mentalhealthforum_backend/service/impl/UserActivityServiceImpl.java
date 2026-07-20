package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserActivityServiceImpl implements UserActivityService {

    private  static final Logger log = LoggerFactory.getLogger(UserActivityServiceImpl.class);

    private final AppUserRepository appUserRepository;

    public UserActivityServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Updates the user's last login timestamp after successful authentication
     * This is a separate from the sync to keep concerns clean
     */
    @Override
    public Mono<Void> recordLoginActivity(UUID userId){
        return appUserRepository.recordLoginActivity(userId, Instant.now())
                .doOnSuccess(updated -> {
                    if(updated > 0){
                        log.debug("Recorded login activity for user {}", userId);
                    }
                    else {
                        log.warn("User not found for login activity: {}", userId);
                    }
                })
                .doOnError(e -> log.error("Failed to record login activity for user {}: {}", userId, e.getMessage()))
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

    /**
     * Tracks user activity by updating last_active_at.
     * Should be called on login, token refresh, and API requests.
     * Non-blocking – errors are logged but ignored.
     * */
    @Override
    public Mono<Void> trackActivity(UUID userId){
        return  appUserRepository.updateLastActive(userId, Instant.now())
                .doOnSuccess(updated -> {
                    if(updated > 0){
                        log.debug("Updated last_active_at for user {}", userId);
                    }
                    else {
                        log.warn("User not found for activity tracking: {}", userId);
                    }
                })
                .doOnError(e -> log.error("Failed to track activity for user {}: {}", userId, e.getMessage()))
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

    /**
     * Activates a user account on login
     * Set is_active = true and updates last_active_at
     */
    @Override
    public Mono<Void> activateUser(UUID userId){
        return  appUserRepository.activateUser(userId)
                .doOnSuccess(updated -> {
                    if(updated > 0){
                        log.debug("Activated user: {}", userId);
                    }
                    else {
                        log.warn("User not found for activation: {}", userId);
                    }
                })
                .doOnError(e -> log.error("Failed to activate user {}: {}", userId, e.getMessage()))
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

    /**
     * Activates a user account on login
     * Set is_active = true and updates last_active_at
     */
    @Override
    public Mono<Void> deactivateUser(UUID userId){
        return  appUserRepository.deactivateUser(userId)
                .doOnSuccess(updated -> {
                    if(updated > 0){
                        log.debug("Deactivated user: {}", userId);
                    }
                    else {
                        log.warn("User not found for deactivation: {}", userId);
                    }
                })
                .doOnError(e -> log.error("Failed to deactivate user {}: {}", userId, e.getMessage()))
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

}
