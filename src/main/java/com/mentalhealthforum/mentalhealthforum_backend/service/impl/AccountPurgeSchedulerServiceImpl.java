package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AppUserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;

@Service
public class AccountPurgeSchedulerServiceImpl implements AccountPurgeSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeSchedulerServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final UserService userService;


    public AccountPurgeSchedulerServiceImpl(
            AppUserRepository appUserRepository,
            UserService userService) {
        this.appUserRepository = appUserRepository;
        this.userService = userService;
    }

    /**
     * Orchestrates the purging of expired accounts.
     * Called by DatabaseCleanupTask or admin trigger.
     * Returns the number of accounts processed
     */
    @Override
    public Mono<Integer> purgeExpiredAccounts() {

        return appUserRepository.findPendingDeletionAccounts(Instant.now())
                .flatMap(appUser -> {
                    String userId = appUser.getKeycloakId().toString();

                    return userService.permanentlyDeleteUser(userId)
                            .then(Mono.defer(() -> anonymizeUser(appUser)))
                            .onErrorResume(e -> {
                                log.error("Failed to purge user {}: {}", userId, e.getMessage());

                                // Try to anonymize even if keycloak fails
                                return anonymizeUser(appUser);
                            })
                            .thenReturn(1) // Count this user as successfully processed
                            .onErrorResume(e -> Mono.just(0)); // Don't count if failed
                })
                .reduce(0, Integer::sum)
                .defaultIfEmpty(0);
    }

    private Mono<Void> anonymizeUser(AppUserEntity appUser){
        if(appUser.isPendingDeletion()){
            appUser.anonymize();
            appUser.setPurgedAt(Instant.now());

            log.info("Anonymizing user {} after purge", appUser.getKeycloakId());

            return appUserRepository.save(appUser).then();

        }

        if(appUser.isPurged()){
            return Mono.empty();
        }

        // Should never happen - log as error but don't fail the batch
        log.error("Cannot anonymize ACTIVE user: {} (must be PENDING_DELETION OR PURGED)", appUser.getKeycloakId());
        return Mono.empty(); // Silent fail - the job continues
    }
}
