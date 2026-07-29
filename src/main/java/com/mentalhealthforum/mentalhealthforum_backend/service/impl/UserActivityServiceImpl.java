package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

    /**
     * Activates a user account on login
     * Set is_active = true and updates last_active_at
     */
    @Override
    public Mono<Void> reactivateUser(UUID userId){
        return  appUserRepository.reactivateUser(userId)
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

    /**
     * Activates a user account on login
     * Set is_active = true and updates last_active_at
     */
    @Override
    public Mono<Void> deactivateUser(UUID userId){
        Instant requestedAt = Instant.now();
        Instant scheduledAt = requestedAt.plus(AppConstants.ACCOUNT_DELETION_RETENTION_WINDOW); // Configurable retention window

        return  appUserRepository.deactivateUser(userId, requestedAt, scheduledAt)
                .onErrorResume(e ->  Mono.empty())
                .then();
    }

}
