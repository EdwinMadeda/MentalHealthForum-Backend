package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class VerificationTokenConstants {

    private VerificationTokenConstants() {
        // Prevent instantiation
    }

    // ============================================================
    // VERIFICATION TOKEN CONFIGURATION
    // ============================================================

    /**
     * How long a verification token is valid (24 hours)
     */
    public static final Duration TOKEN_EXPIRY_DURATION_HOURS = Duration.ofHours(24);
    public static final long TOKEN_EXPIRY_HOURS = TOKEN_EXPIRY_DURATION_HOURS.toHours();
    public static final long TOKEN_EXPIRY_SECONDS = TOKEN_EXPIRY_DURATION_HOURS.getSeconds();

    /**
     * Rate limiting: how long a user must wait before requesting a new verification email
     */
    public static final Duration TOKEN_RATE_LIMIT_DURATION_MINUTES = Duration.ofMinutes(2);
    public static final long TOKEN_RATE_LIMIT_MINUTES = TOKEN_RATE_LIMIT_DURATION_MINUTES.toMinutes();
    public static final long TOKEN_RATE_LIMIT_SECONDS = TOKEN_RATE_LIMIT_DURATION_MINUTES.getSeconds();

    /**
     * Maximum number of verification tokens per email (to prevent abuse)
     * Keep it low since verification is typically a one-time event
     */
    public static final int MAX_TOKENS_PER_EMAIL = 1;
}