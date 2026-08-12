package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.NovuSubscriberData;
import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.NovuSubscriberRequest;

import java.time.Instant;

/**
 * DTO representing the essential authoritative user data extracted from the
 * external Identity Provider's AppUserEntity Representation payload.
 * This is used INTERNALLY within the service layer for mapping and audit.
 */
public record KeycloakUserDto(
        // Primary key used by the application
        String userId,

        // Basic Profile Information
        String username,
        String firstName,
        String lastName,
        String email,

        // Status and Audit Fields
        boolean enabled,
        boolean emailVerified,

        // The timestamp needs to be handled carefully, assuming it's milliseconds
        // since epoch as commonly used in Java/Keycloak (1763153069993 in your sample).
        @JsonProperty("createdTimestamp")
        Long createdTimestampMs
) {
    /**
     * Helper method to convert the milliseconds timestamp to a more usable Instant object.
     * @return Instant object representing the user's creation time in Keycloak.
     */
    public Instant getCreatedInstant() {
        return  (createdTimestampMs == null)? null : Instant.ofEpochMilli(createdTimestampMs);
    }

    /**
     * Converts this KeycloakUserDto to a Novu subscriber request.
     * Uses the Keycloak user ID as the subscriber ID.
     */
    public NovuSubscriberRequest toNovuSubscriberRequest() {
        return new NovuSubscriberRequest(
                this.userId,
                this.firstName,
                this.lastName,
                this.email,
                null,   // avatarUrl – not available in Keycloak DTO
                "en",   // language – default
                NovuSubscriberData.forNewUserInKeycloak()
        );
    }
}