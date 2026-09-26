package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.UserDoesNotExistException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AdminInvitationEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.*;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.AccountPurgeSchedulerService;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.MfaStateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DatabaseCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupTask.class);

    private final OtpCredentialRepository otpCredentialRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PendingUserRepository pendingUserRepository;
    private final CategoryService categoryService;
    private final ThreadRepository threadRepository;
    private final UserConnectRepository userConnectRepository;
    private final AccountPurgeSchedulerService accountPurgeSchedulerService;
    private final MfaStateCache mfaStateCache;
    private final KeycloakAdminManager adminManager;
    private final KeycloakUserDtoMapper keycloakUserDtoMapper;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final AdminInvitationRepository adminInvitationRepository;
    private final AdminInvitationService adminInvitationService;


    public DatabaseCleanupTask(
            OtpCredentialRepository otpCredentialRepository,
            VerificationTokenRepository verificationTokenRepository,
            PendingUserRepository pendingUserRepository,
            CategoryService categoryService,
            ThreadRepository threadRepository,
            UserConnectRepository userConnectRepository,
            AccountPurgeSchedulerService accountPurgeSchedulerService,
            MfaStateCache mfaStateCache,
            KeycloakAdminManager adminManager,
            KeycloakUserDtoMapper keycloakUserDtoMapper,
            AppUserRepository appUserRepository,
            AppUserService appUserService, AdminInvitationRepository adminInvitationRepository,
            AdminInvitationService adminInvitationService) {
        this.otpCredentialRepository = otpCredentialRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.pendingUserRepository = pendingUserRepository;
        this.categoryService = categoryService;
        this.threadRepository = threadRepository;
        this.userConnectRepository = userConnectRepository;
        this.accountPurgeSchedulerService = accountPurgeSchedulerService;
        this.mfaStateCache = mfaStateCache;
        this.keycloakUserDtoMapper = keycloakUserDtoMapper;
        this.adminManager = adminManager;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
        this.adminInvitationRepository = adminInvitationRepository;
        this.adminInvitationService = adminInvitationService;
    }

    // Runs at 3:00 AM every day
    @Scheduled( cron = "0 0 3 * * *")
    public void cleanupExpiredOtps() {
        log.info("Cron: Initiating scheduled cleanup of expired OTP credentials...");

        otpCredentialRepository.deleteAllExpired(Instant.now())
                .doOnSuccess(count -> {
                    if (count > 0) { log.info("Cron Success: Cleanup complete. Removed {} stale records from 'otp_credentials'.", count);}
                    else { log.debug("Cron Success: No expired OTP records found to clean up.");}
                })
                .doOnError(e -> log.error("Cron Failure: Failed to execute OTP cleanup task. Reason: {}", e.getMessage()))
                .block();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupStaleRegistrations(){
        log.info("Cron: Initiating scheduled cleanup of stale pending registrations...");

        // Delete expired tokens first
        verificationTokenRepository.deleteAllExpired(Instant.now())
                .doOnSuccess(count -> {
                    if(count > 0){ log.info("Cron Success: Removed {} expired verification tokens.", count);}
                    else { log.debug("Cron Success: No expired verification tokens found to clean up.");}
                })
                .doOnError(e -> log.debug("Cron Failure: Failed to executed expired verification token cleanup task: Reason: {}", e.getMessage()))
                .block();

        // 2. Delete pending users who have NO associated token
        // (This implies their 24h window closed)
        pendingUserRepository.deleteOrphanedPendingUsers()
                .doOnSuccess(count -> {
                    if(count > 0){ log.info("Cron Success: Removed {} stale pending user records.", count);}
                    else {log.info("Cron Success: No expired pending user records found to clean up.");}
                })
                .doOnError(e -> log.debug("Cron Failure: Failed to executed pending users token cleanup task: Reason: {}", e.getMessage()))
                .block();

    }

    // Run at 2 AM daily
    @Scheduled(cron = "0 0 2 * * ?")
    public void purgeOldInactiveCategories(){
        log.info("Starting scheduled purge of old inactive categories");
        categoryService.purgeOldInactiveCategoriesInternal(90)
                .subscribe(
                        v -> log.info("Scheduled purge completed"),
                        e -> log.error("Scheduled purge failed: {}", e.getMessage())
                );
    }

    // Runs Every minute
    @Scheduled(cron = "0 * * * * *")
    public void unlockExpiredThreads(){
        threadRepository.unlockExpiredThreads()
                .subscribe(
                        count -> log.info("Unlock {} expired threads", count)
                );
    }

    // Run at 2 AM daily
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDeclinedConnections(){
        log.info("Starting cleanup of declined connections older than 30 days");
        userConnectRepository.deleteDeclinedOlderThan(30)
                .subscribe(
                    count -> log.info("Cleaned up {} declined connections", count),
                        error -> log.error("Error cleaning up declined connections: {}", error.getMessage())
                );
    }

    // Runs daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredAccounts(){
        log.info("Starting scheduled purge of expired accounts");

        accountPurgeSchedulerService.purgeExpiredAccounts()
                .doOnSuccess(count -> log.info("Scheduled purge completed. Processed {} accounts.", count))
                .doOnError(e -> log.error("Scheduled purge failed: {}", e.getMessage(), e))
                .subscribe(); // Fire and forget

    }

    // Runs every 5 minutes
    public void cleanUpExpiredMfaSession() {
        log.debug("Starting cleanup of expired MFA states");
        mfaStateCache.cleanup();
    }

    // Syncs all app users with Keycloak. Runs at 3 AM daily.
    @Scheduled(cron = "0 0 3 * * *")
    public void syncAppUsers(){
        log.info("Cron: Initiating scheduled sync of app users");

        appUserRepository.findAll()
                .flatMap(appUser -> Mono.fromCallable(()-> adminManager.findUserByUserId(
                        appUser.getKeycloakId().toString())
                        .orElseThrow(()-> new UserDoesNotExistException("User not found"))
                ).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(userRep -> appUserService.syncUserViaAdminClient(
                            keycloakUserDtoMapper.mapToKeycloakUserDto(userRep),
                                null
                ))
                .doOnComplete(()-> log.info("Cron Success: Completed sync of app users"))
                .doOnError(e-> log.error("Cron Failure: App user sync failed: {}", e.getMessage()))
                .subscribe();

    }

    // Syncs pending invitations with keycloak. Runs at 2 AM daily.
    @Scheduled(cron = "0 0 2 * * *")
    public void syncPendingInvitations(){
        log.info("Cron: Initiating scheduled sync of pending invitations");

        adminInvitationRepository.findAll()
                .flatMap(invitation -> adminInvitationService.syncPendingInviteFromKeycloak(
                        invitation.getKeycloakId().toString()
                ))
                .doOnComplete(()-> log.info("Cron Success: Completed sync of pending invitations"))
                .doOnError(e-> log.error("Cron Failure: Pending invitation failed: {}", e.getMessage()))
                .subscribe();

    }

    // Purges expired invitations that haven't been touched. Runs at 4 AM daily
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeExpiredInvitations(){
        log.info("Cron: Initiating scheduled purge of expired invitations");

        adminInvitationRepository.findAll()
                .filter(AdminInvitationEntity::isEligibleForPurge)
                .flatMap(adminInvitationService::purgeExpiredInvitation)
                .doOnComplete(()-> log.info("Cron success: Completed purge of expired invitations"))
                .doOnError(e-> log.error("Cron Failure: Expired invitation purge failed: {}", e.getMessage()))
                .subscribe();

    }


}
