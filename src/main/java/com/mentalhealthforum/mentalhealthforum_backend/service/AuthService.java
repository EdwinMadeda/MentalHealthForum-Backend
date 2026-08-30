package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.AuthResult;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.JwtResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.LoginRequest;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<AuthResult> authenticate(LoginRequest request, String ipAddress, String userAgent);
    Mono<JwtResponse> refreshTokens(String refreshToken);

    /**
     * Logs out the user by revoking the refresh token in Keycloak,
     * invalidating the session immediately.
     * @param refreshToken The refresh token to be revoked.
     * @return Mono<Void> indicating the operation is complete (Success or handled failure).
     */
    Mono<Void> logout(String refreshToken);

    Mono<JwtResponse> issueFullTokens(String stateToken);

}
