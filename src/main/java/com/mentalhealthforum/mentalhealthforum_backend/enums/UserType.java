package com.mentalhealthforum.mentalhealthforum_backend.enums;

/**
 * Discriminator for the union DTO {@code AdminUserDetailsDto<T>}.
 *
 * <p>Indicates whether the wrapped result is:
 * <ul>
 *   <li>{@code SYNCED} - A fully onboarded user from {@code app_users}</li>
 *   <li>{@code PENDING} - A pending user from {@code admin_invitations}</li>
 * </ul>
 */
public enum UserType {
    SYNCED,
    PENDING
}
