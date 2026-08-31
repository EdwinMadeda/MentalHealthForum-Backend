package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class MfaConstants {

    private MfaConstants() {
        // Prevent instantiation
    }

    // ============================================================
    // BACKUP CODE CONFIGURATION
    // ============================================================

    /**
     * Number of backup codes to generate for each user
     */
    public static final int MFA_BACKUP_CODE_COUNT = 10;

    /**
     * Length of each backup code
     */
    public static final int MFA_BACKUP_CODE_LENGTH = 8;

    // ============================================================
    // STATE TOKEN CONFIGURATION
    // ============================================================

    /**
     * How long an MFA state token is valid (10 minutes)
     */
    public static final Duration MFA_STATE_TOKEN_EXPIRY_DURATION_MINUTES = Duration.ofMinutes(10);
    public static final long STATE_TOKEN_EXPIRY_SECONDS = MFA_STATE_TOKEN_EXPIRY_DURATION_MINUTES.getSeconds();

    // ============================================================
    // MFA SETUP CONFIGURATION
    // ============================================================

    /**
     * Whether MFA is mandatory for admin users
     */
    public static final boolean ENFORCE_MFA_FOR_ADMINS = true;

    // ============================================================
    // REMEMBER ME CONFIGURATION
    // ============================================================

    /**
     * How long to remember MFA trust for a device (30 days)
     */
    public static final Duration MFA_REMEMBER_ME_DURATION_DAYS = Duration.ofDays(30);
    public static final long REMEMBER_ME_SECONDS = MFA_REMEMBER_ME_DURATION_DAYS.getSeconds();

}