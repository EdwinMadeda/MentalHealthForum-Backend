package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class MfaConstants {

    private MfaConstants() {
        // Prevent instantiation
    }

    // ============================================================
    // BACKUP CODE CONFIGURATION
    // ============================================================
    public static final int MFA_BACKUP_CODE_COUNT = 10;
    public static final int MFA_BACKUP_CODE_LENGTH = 8;

    // ============================================================
    // STATE TOKEN CONFIGURATION
    // ============================================================
    public static final Duration MFA_STATE_TOKEN_EXPIRY_DURATION_MINUTES = Duration.ofMinutes(10);
    public static final int MFA_STATE_TOKEN_EXPIRY_SECONDS = (int) MFA_STATE_TOKEN_EXPIRY_DURATION_MINUTES.getSeconds();

    // ============================================================
    // MFA SETUP CONFIGURATION
    // ============================================================
    public static final boolean ENFORCE_MFA_FOR_ADMINS = true;

    // ============================================================
    // REMEMBER ME CONFIGURATION
    // ============================================================
    public static final Duration MFA_REMEMBER_ME_DURATION_DAYS = Duration.ofDays(30);
    public static final int MFA_REMEMBER_ME_SECONDS = (int) MFA_REMEMBER_ME_DURATION_DAYS.getSeconds();


}
