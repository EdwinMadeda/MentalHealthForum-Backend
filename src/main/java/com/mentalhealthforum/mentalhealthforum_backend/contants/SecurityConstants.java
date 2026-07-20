package com.mentalhealthforum.mentalhealthforum_backend.contants;

public class SecurityConstants {

    private SecurityConstants() {} // Prevent instantiation

    public static final String[] AUTH_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error/**",
            "/api/users/register/**",
            "/api/auth/**", // Consolidated authentication paths for login, refresh, and logout
            "/api/timezones/**"
    };
}
