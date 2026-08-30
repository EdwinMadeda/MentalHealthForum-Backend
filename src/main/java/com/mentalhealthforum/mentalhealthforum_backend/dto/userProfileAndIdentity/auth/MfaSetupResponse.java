package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

public record MfaSetupResponse(
        boolean setupRequired,
        String email,
        int otpLength,
        int expirySeconds
) {}
