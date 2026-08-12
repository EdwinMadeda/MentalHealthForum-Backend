package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user;

public record RegistrationResult(
    String email,
    boolean emailSent
) {}
