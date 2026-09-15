package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;

import java.util.UUID;

public record UserAuditReasonDefinitionDto(
    UUID id,
    UserAuditReasonKey key,                     // EXCEPTIONAL_CONTRIBUTION, etc.
    String description,                     // "Exceptional contribution to the community"
    UserAuditAction actionType,             // PROMOTED, DEMOTED, etc.
    boolean isActive,
    int sortOrder
) {}
