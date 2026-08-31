package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

public record MfaSetupResult(
        boolean setupRequired,
        String stateToken,
        String email,
        int otpLength,
        long expirySeconds
) {
    public MfaSetupResponse toResponse(){
        return new MfaSetupResponse(
                true,
                email,
                otpLength,
                expirySeconds
        );
    }
}
