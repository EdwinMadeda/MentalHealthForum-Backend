package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class OtpConstants {

    private OtpConstants() {
        // Prevent instantiation
    }

    // ============================================================
    // OTP CONFIGURATION
    // ============================================================
    public static final int OTP_LENGTH = 6;
    public static final Duration OTP_EXPIRY_DURATION_MINUTES = Duration.ofMinutes(10);
    public static final int  OTP_EXPIRY_MINUTES = (int) OTP_EXPIRY_DURATION_MINUTES.toMinutes();
    public static final int OTP_EXPIRY_SECONDS = (int) OTP_EXPIRY_DURATION_MINUTES.getSeconds();
    public static final Duration OTP_RATE_LIMIT_DURATION_SECONDS = Duration.ofSeconds(60);
    public static final int OTP_RATE_LIMIT_SECONDS = (int) OTP_RATE_LIMIT_DURATION_SECONDS.toSeconds();
    public static final int OTP_MAX_ATTEMPTS = 5;
    public static final Duration OTP_LOCKOUT_DURATION_MINUTES = Duration.ofMinutes(15);

}
