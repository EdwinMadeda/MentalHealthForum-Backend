package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Contract for entities/DTOs that have an expiry and purge eligibility.
 *
 * <p>Implementations must provide:
 * <ul>
 *   <li>{@code expiresAt} - The expiry timestamp</li>
 *   <li>{@code isEmailVerified} - Whether email is verified</li>
 *   <li>{@code currentStage} - The onboarding stage</li>
 * </ul>
 *
 * <p>Default methods compute:
 * <ul>
 *   <li>{@code isExpired} - Whether the entity has passed its expiry time</li>
 *   <li>{@code isEligibleForPurge} - Whether the entity is eligible for purge</li>
 * </ul>
 */
 public interface InviteExpirable {

    @JsonIgnore
    Instant getExpiresAt();

    @JsonIgnore
    boolean getIsEmailVerified();

    @JsonIgnore
    OnboardingStage getCurrentStage();

    @JsonProperty("is_expired")
    default boolean isExpired(){
        Instant now = Instant.now();
        return getExpiresAt() != null && getExpiresAt().isBefore(now);
    }

    @JsonProperty("is_eligible_for_purge")
    default boolean isEligibleForPurge(){

        Instant now = Instant.now();
        Instant graceThreshold = now.minus(AppConstants.INVITE_PURGE_GRACE_HOURS, ChronoUnit.HOURS);

        // Must be expired
        if(getExpiresAt() == null || !getExpiresAt().isBefore(graceThreshold)){
            return false;
        }

        // Must have verified email
        if(getIsEmailVerified()){
            return false;
        }

        // Must still be in AWAITING_VERIFICATION stage
        return getCurrentStage() == OnboardingStage.AWAITING_VERIFICATION;

    }

    /**
     * Calculates a new expiry timestamp from now.
     * Does not modify the entity - returns the new value.
     */
    default Instant newExpiryTimestamp(){
        return Instant.now().plus(AppConstants.INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

}
