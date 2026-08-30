package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaPendingState;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaSetupResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.MfaSetupResult;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import reactor.core.publisher.Mono;

public interface MfaService {

    Mono<MfaSetupResult> setupMfa(String userId, String ipAddress, String userAgent);;

    Mono<MfaPendingState> initiateMfaChallenge(AppUserEntity appUser, String ipAddress, String userAgent, String accessToken, String refreshToken);

    Mono<AppUserEntity> verifyMfaOtp(String stateToken, String otpCode);

    Mono<AppUserEntity> enableMfa(AppUserEntity appUser);

    Mono<AppUserEntity> disableMfa(AppUserEntity appUser);

    Mono<Void> recoverWithBackupCode(String stateToken, String backupCode);
}
