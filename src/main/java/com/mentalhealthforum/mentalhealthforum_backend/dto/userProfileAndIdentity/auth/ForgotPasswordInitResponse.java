package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_EXPIRY_SECONDS;
import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_LENGTH;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.MaskEmailUtils.maskEmail;

public record ForgotPasswordInitResponse(
    boolean otpSent,
    String email,
    int otpLength,
    int expirySeconds
) {
    public static ForgotPasswordInitResponse forEmail(String email){
        return new ForgotPasswordInitResponse(
                 true,
                maskEmail(email),
                OTP_LENGTH,
                OTP_EXPIRY_SECONDS
        );
    }
}
