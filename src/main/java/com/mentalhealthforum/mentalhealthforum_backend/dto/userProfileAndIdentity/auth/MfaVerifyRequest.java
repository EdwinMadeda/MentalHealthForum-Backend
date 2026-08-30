package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

import com.mentalhealthforum.mentalhealthforum_backend.validation.otp.ValidOtp;
import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank(message = "OTP code is required")
        @ValidOtp
        String otpCode
) {}
