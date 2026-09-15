package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;

import java.util.List;
import java.util.UUID;

public record UserAuditReasonDefinitionGroupedDto(
   UserAuditAction actionType,
   String actionDisplayName,
   List<UserAuditReasonDefinitionDto> reasons
) {}
