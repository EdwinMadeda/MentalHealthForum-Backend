package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum AccountStatus {
    /**
     * Normal active account. User can log in, participate, and access all features.
     */
    ACTIVE,

    /**
     * Admin imposed permanent ban
     */
    BANNED,

    /**
     * User requested deletion. Account is deactivated but data is preserved for the retention window.
     * User can cancel deletion and reactivate
     */
    PENDING_DELETION,

    /**
     * Account permanently deleted. Personal data is purged or anonymized
     * Contributions remain but are attributed to "Deleted user"
     * Account cannot be reactivated
     */
    PURGED
}
