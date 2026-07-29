package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.time.Duration;

public class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ========== USER DELETION ==========
    /**
     * Retention window for pending deletion accounts.
     * Users have this long to cancel their deletion request.
     * After this period, the account is permanently purged.
     */
    public static final Duration ACCOUNT_DELETION_RETENTION_WINDOW = Duration.ofDays(30);

    /**
     * When true, other users can see profiles of admins and moderators can see
     * for transparency purposes.
     */
    public static final boolean ENFORCE_ADMIN_TRANSPARENCY = true;

    // ========== ACTIVITY TRACKING ==========
    /**
     * Cooldown period for updating last_active_at.
     * Prevents write-storms on high-traffic endpoints.
     */
    public static final Duration ACTIVITY_UPDATE_THRESHOLD = Duration.ofMinutes(5);

}
