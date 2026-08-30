package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

import java.time.Instant;
import java.util.UUID;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_MAX_ATTEMPTS;

public record MfaPendingState(
        String stateToken,
        UUID userId,
        String email,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        String ipAddress,
        String userAgent,
        int failedAttempts
) {

    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isMaxedOut() {
        return failedAttempts >= OTP_MAX_ATTEMPTS;
    }

    public MfaPendingState incrementFailure(){
        return new MfaPendingState(
                stateToken,
                userId,
                email,
                accessToken,
                refreshToken,
                expiresAt,
                ipAddress,
                userAgent,
                failedAttempts + 1
        );
    }

}
