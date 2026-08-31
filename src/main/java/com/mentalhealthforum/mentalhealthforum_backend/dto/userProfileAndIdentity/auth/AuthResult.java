package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

public sealed interface AuthResult {

    record Success(JwtResponse jwtResponse) implements AuthResult {}

    record MfaRequired(String stateToken, String email, int otpLength, long expirySeconds) implements AuthResult {}

}
