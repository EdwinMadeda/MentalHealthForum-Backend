package com.mentalhealthforum.mentalhealthforum_backend.contants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SecurityConstants {

    private SecurityConstants() {} // Prevent instantiation

    // ====== RAW WHITELIST PATTERNS (for SecurityConfig pathMatchers) ======
    public static final String[] AUTH_WHITELIST = {
            // API Documentation
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // Monitoring
            "/actuator/**",
            "/actuator/health/**",
            "/health",
            "/api/health",

            // Error Handling
            "/error/**",

            // Authentication
            "/api/auth/**", // Login, refresh, logout

            // User Registration & Verification
            "/api/users/register/**",
            "/api/users/verify/**",
            "/api/users/reset-password/**",

            // Public Utilities
            "/api/timezones/**",

            // Static Assets
            "/favicon.ico",
            "/robots.txt",
            "/static/**",
            "/assets/**"
    };

    // ====== PREFIX LIST (for AccessAuthorizationManager & ActivityTrackingFilter) ======
    public static final List<String> PUBLIC_PATHS = Arrays.stream(AUTH_WHITELIST)
            .map(pattern -> pattern.replace("/**", "")) // Convert /** to prefix match
            .collect(Collectors.toList());

    // ====== REGEX PATTERNS (for specific matching) ======
    public static final String REACTIVATION_PATH_REGEX = "/api/users/[a-f0-9-]+/reactivate";
}
