package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum OnboardingStage {
    AWAITING_VERIFICATION("Awaiting Verification"),
    AWAITING_PASSWORD_RESET("Awaiting Password Reset"),
    AWAITING_PROFILE_COMPLETION("Awaiting Profile Completion");

    private final String displayName; // Human-readable name

    OnboardingStage(String displayName) {
        this.displayName = displayName;
    }
}
