package com.mentalhealthforum.mentalhealthforum_backend.exception.error;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import lombok.Getter;

@Getter
public class MfaRequiredException extends ApiException {
    private final String stateToken;
    private final String email;
    private final int otpLength;
    private final int expirySeconds;

    public MfaRequiredException(String message, String stateToken, String email, int otpLength, int expirySeconds) {
        super(message, ErrorCode.MFA_REQUIRED);
        this.stateToken = stateToken;
        this.email = email;
        this.otpLength = otpLength;
        this.expirySeconds = expirySeconds;
    }

}