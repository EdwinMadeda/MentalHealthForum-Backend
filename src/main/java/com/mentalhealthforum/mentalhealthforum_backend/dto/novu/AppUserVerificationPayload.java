package com.mentalhealthforum.mentalhealthforum_backend.dto.novu;

import com.mentalhealthforum.mentalhealthforum_backend.service.NovuPayload;

import java.util.Map;

public record AppUserVerificationPayload(
        String firstName,
        String verificationLink,
        boolean isEmailChange
) implements NovuPayload {
    @Override
    public Map<String, Object> toPayloadMap() {
        return Map.of(
                "first_name", firstName,
                "verification_link", verificationLink,
                "is_email_change", isEmailChange
        );
    }
}
