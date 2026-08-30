package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.AdminMfaOtpPayload;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaPendingState;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaSetupResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaSetupResult;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.enums.NovuWorkflow;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OtpPurpose;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.*;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.MfaService;
import com.mentalhealthforum.mentalhealthforum_backend.service.NovuService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.util.List;
import java.util.UUID;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_EXPIRY_SECONDS;
import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_LENGTH;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.BackupCodeUtils.generateBackupCodes;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.BackupCodeUtils.hashBackupCodes;


@Service
public class MfaServiceImpl implements MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final MfaStateCache mfaStateCache;
    private final NovuService novuService;
    private final OtpWorker otpWorker;
    private final PasswordEncoder passwordEncoder;

    public MfaServiceImpl(
            AppUserRepository appUserRepository,
            MfaStateCache mfaStateCache,
            NovuService novuService,
            OtpWorker otpWorker,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.mfaStateCache = mfaStateCache;
        this.novuService = novuService;
        this.otpWorker = otpWorker;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<MfaSetupResult> setupMfa(String userId, String ipAddress, String userAgent){

        return appUserRepository.findAppUserByKeycloakId((userId))
                .switchIfEmpty(Mono.error(new UserDoesNotExistException()))
                .flatMap(appUser -> {
                    if(appUser.isMfaEnabled()){
                        return Mono.error(new ApiException("MFA is already enabled for this account.", ErrorCode.VALIDATION_FAILED));
                    }

                    // Initiate MFA challenge (sends OTP)
                    return initiateMfaChallenge(appUser, ipAddress, userAgent, null, null)
                            .map(mfaState -> {
                                return new MfaSetupResult(
                                        true,
                                        mfaState.stateToken(),
                                        appUser.getEmail(),
                                        OTP_LENGTH,
                                        OTP_EXPIRY_SECONDS
                                );
                            });
                });
    }

    /**
     * Initiates MFA challenge by generating a state token and sending OTP.
     */
    @Override
    public Mono<MfaPendingState> initiateMfaChallenge(AppUserEntity appUser, String ipAddress, String userAgent, String accessToken, String refreshToken){
        UUID userId = appUser.getKeycloakId();
        String email = appUser.getEmail();
        String firstName = appUser.getFirstName();

        return otpWorker.generateAndSaveOtp(email, OtpPurpose.ADMIN_MFA)
                .flatMap(otpResult -> {
                    String stateToken = mfaStateCache.createState(userId, email, ipAddress, userAgent, accessToken, refreshToken);

                    return novuService.triggerEvent(
                            NovuWorkflow.ADMIN_MFA_OTP,
                            userId.toString(),
                            email,
                            new AdminMfaOtpPayload(firstName, otpResult.code(), otpResult.expiryMinutes())
                    )
                            .doOnSuccess(sent -> {
                                if(sent){
                                    log.info("MFA OTP sent to: {}", email);
                                }
                            })
                            .then(mfaStateCache.getState(stateToken))
                            .switchIfEmpty(Mono.error(new RuntimeException("Failed to create MFA state")));
                })
                .doOnError(e -> log.error("Failed to initiate MFA challenge for user: {}", email, e));
    }


    /**
     * Verifies MFA OTP and issues full tokens.
     * */
    @Override
    public Mono<AppUserEntity> verifyMfaOtp(String stateToken, String otpCode){
        return mfaStateCache.getState(stateToken)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired MFA state")))
                .flatMap(mfaState -> {
                    UUID userId = mfaState.userId();

                    return otpWorker.verifyOtp(mfaState.email(), otpCode, OtpPurpose.ADMIN_MFA)
                            .then(appUserRepository.findAppUserByKeycloakId(userId.toString())
                                    .switchIfEmpty(Mono.error(new UserDoesNotExistException("User not found"))));
                })
                .onErrorResume(e -> {
                    // If it's a structural system error (like a DB crash or UserDoesNotExist), don't penalize the user
                    if(e instanceof UserDoesNotExistException){
                        return Mono.error(e);
                    }

                    // For all credential/token failures (InvalidToken, TokenExpired, etc.), increment failure atomically
                    mfaStateCache.recordFailedAttempt(stateToken);
                    return Mono.error(e);  // Pass the exact exception up to your global handler for clear frontend feedback
                });
    }

    /**
     * Enables MFA for a user and generates backup codes
     * */
    @Override
    public Mono<AppUserEntity> enableMfa(AppUserEntity appUser){
        List<String> rawBackupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = hashBackupCodes(rawBackupCodes, passwordEncoder);

        appUser.enableMfa(hashedBackupCodes);

        return appUserRepository.save(appUser)
                .doOnSuccess(saved -> log.info("MFA enabled for user: {}" , saved.getKeycloakId()))
                .map(saved -> {
                    saved.setRawBackupCodes(rawBackupCodes);
                    return saved;
                });

    }

    /**
     * Disables MFA for a user and generates backup codes
     * */
    @Override
    public Mono<AppUserEntity> disableMfa(AppUserEntity appUser){
        appUser.disableMfa();
        return appUserRepository.save(appUser)
                .doOnSuccess(saved -> log.info("MFA disabled for user: {}" , saved.getKeycloakId()));
    }

    @Override
    public Mono<Void> recoverWithBackupCode(String stateToken, String backupCode){
        return mfaStateCache.getState(stateToken)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired MFA state")))
                .flatMap(mfaState -> {
                    return appUserRepository.findAppUserByKeycloakId(mfaState.userId().toString())
                            .switchIfEmpty(Mono.error(new UserDoesNotExistException()))
                            .flatMap(appUser -> {
                                return verifyBackupCode(appUser, backupCode)
                                        .flatMap(verified -> {
                                            if(!verified){
                                                return Mono.error(new ApiException(
                                                        "Invalid backup code.",
                                                        ErrorCode.VALIDATION_FAILED
                                                ));
                                            }
                                            return Mono.empty();
                                        });
                            });
                });

    }

    /**
     * Verifies a backup code and marks it as used.
     */
    private Mono<Boolean> verifyBackupCode(AppUserEntity appUser, String backupCode) {
        List<String> hashedCodes = appUser.getMfaHashedBackupCodesList();
        if(hashedCodes.isEmpty()){
            return Mono.just(false);
        }

        for(String hashedCode: hashedCodes){
            if(passwordEncoder.matches(backupCode, hashedCode)){
                // Remove the used backup code
                hashedCodes.remove(hashedCode);
                appUser.setMfaHashedBackupCodesList(hashedCodes);
                return appUserRepository.save(appUser)
                        .map(saved -> true);
            }
        }
        return Mono.just(false);
    }


}
