package com.mentalhealthforum.mentalhealthforum_backend.dto.novu;

import com.mentalhealthforum.mentalhealthforum_backend.service.NovuPayload;

import java.util.Map;

public record AdminMfaOtpPayload(String firstName, String code, int expiryMinutes) implements NovuPayload {
    @Override
    public Map<String, Object> toPayloadMap() {
        return Map.of(
                    "firstName", firstName,
                "otpCode", code,
                "expiryMinutes", expiryMinutes
        );
    }
}
