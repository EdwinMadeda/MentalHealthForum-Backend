package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Table(name = "admin_invitations")
public class AdminInvitationEntity {
    @Id
    @Column("id")
    private UUID id; // DB-generated UUID Primary Key

    // --- Keycloak-Owned Identity Data (Requires Synchronization) ---

    @Column("keycloak_id")
    @NotNull
    private UUID keycloakId; // External identifier provided by Keycloak

    // --- Authoritative Keycloak-synced identity fields ---
    @Email
    @Column("email")
    @NotBlank
    private String email;

    @Column("username")
    @NotBlank
    private String username;

    @Column("first_name")
    @NotBlank
    private String firstName;

    @Column("last_name")
    @NotBlank
    private String lastName;

    // --- Cached/display-only fields (Postgres-compatible storage: text[] or jsonb) ---
    @Column("groups")
    private Set<String> groups = new HashSet<>();

    // -- Others also synced from keycloak
    @Column("is_enabled")
    private Boolean isEnabled = true;

    @Column("is_email_verified")
    private Boolean isEmailVerified = false;

    @Column("date_created")
    private Instant dateCreated;

    // --- Application-specific data

    @Column("invited_by")
    private UUID invitedBy;

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

    @Column("current_stage")
    private OnboardingStage currentStage = OnboardingStage.AWAITING_VERIFICATION;

    @Column("is_initial_login")
    private Boolean isInitialLogin = true;

    @Column("expires_at")
    private Instant expiresAt;

    // Checks if this invitation has passed its expiry time.
    @Transient
    public boolean isExpired(){
        Instant now = Instant.now();
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    // Checks if this invitation should be purged.
    @Transient
    public boolean isEligibleForPurge(){

        Instant now = Instant.now();
        Instant graceThreshold = now.minus(AppConstants.INVITE_PURGE_GRACE_HOURS, ChronoUnit.HOURS);

        // Must be expired
        if(this.expiresAt == null || !this.expiresAt.isBefore(graceThreshold)){
            return false;
        }

        // Must have verified email
        if(Boolean.TRUE.equals(this.isEmailVerified)){
            return false;
        }

        // Must still be in AWAITING_VERIFICATION stage
        return this.currentStage == OnboardingStage.AWAITING_VERIFICATION;

    }

    // Returns the groups as an array of GroupPath enums. Converts from stored paths.
    @Transient
    public GroupPath[] getGroupPaths() {
        return  this.groups != null
                ? this.groups.stream()
                .map(GroupPath::fromPath)
                .filter(Objects::nonNull)
                .toArray(GroupPath[]::new)
                : new GroupPath[0];
    }

    // Constructor
    public AdminInvitationEntity(
            String keycloakStringId,
            String email,
            String username,
            String firstName,
            String lastName,
            Set<String> groups,
            Instant dateCreated,
            String invitedByStringId
    ) {
        if(keycloakStringId == null || keycloakStringId.trim().isEmpty()){
            throw new IllegalArgumentException("keycloakStringId cannot be null or empty");
        }
        if(invitedByStringId == null || invitedByStringId.trim().isEmpty()){
            throw new IllegalArgumentException("invitedByStringId cannot be null or empty");
        }

        this.keycloakId = UUID.fromString(keycloakStringId);
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.groups = groups;
        this.dateCreated = dateCreated;
        this.invitedBy = UUID.fromString(invitedByStringId);

        this.expiresAt = this.newExpiryTimestamp();
    }

    // Calculates a new expiry timestamp from now.
    private Instant newExpiryTimestamp(){
        return Instant.now().plus(AppConstants.INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

     // Resets the expiry timestamp to a new window from now.
    public void resetExpiry(){
        this.expiresAt = this.newExpiryTimestamp();
    }

}
