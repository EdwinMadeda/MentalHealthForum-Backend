package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

public record MfaChallengeResponse(
        boolean mfaRequired,
        String email,
        int otpLength,
        int expirySeconds
) {}
