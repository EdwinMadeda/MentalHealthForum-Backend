package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class OtpConstants {

    private OtpConstants() {
        // Prevent instantiation
    }

    // ============================================================
    // OTP CONFIGURATION
    // ============================================================

    /**
     * Number of digits in the OTP code
     */
    public static final int OTP_LENGTH = 6;

    /**
     * How long an OTP is valid (10 minutes)
     */
    public static final Duration OTP_EXPIRY_DURATION_MINUTES = Duration.ofMinutes(10);
    public static final long OTP_EXPIRY_MINUTES = OTP_EXPIRY_DURATION_MINUTES.toMinutes();
    public static final long OTP_EXPIRY_SECONDS = OTP_EXPIRY_DURATION_MINUTES.getSeconds();

    /**
     * Rate limiting: how long a user must wait before requesting a new OTP (60 seconds)
     */
    public static final Duration OTP_RATE_LIMIT_DURATION_SECONDS = Duration.ofSeconds(60);
    public static final long OTP_RATE_LIMIT_SECONDS = OTP_RATE_LIMIT_DURATION_SECONDS.toSeconds();

    /**
     * Maximum number of failed OTP attempts before lockout
     */
    public static final int OTP_MAX_ATTEMPTS = 5;

    /**
     * How long to lock out a user after max failed attempts (15 minutes)
     */
    public static final Duration OTP_LOCKOUT_DURATION_MINUTES = Duration.ofMinutes(15);
    public static final long OTP_LOCKOUT_MINUTES = OTP_LOCKOUT_DURATION_MINUTES.toMinutes();
    public static final long OTP_LOCKOUT_SECONDS = OTP_LOCKOUT_DURATION_MINUTES.getSeconds();

}