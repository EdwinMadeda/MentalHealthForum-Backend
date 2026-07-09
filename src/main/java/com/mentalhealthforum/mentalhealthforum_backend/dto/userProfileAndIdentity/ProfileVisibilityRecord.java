package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity;

import java.util.UUID;

public record ProfileVisibilityRecord(UUID userId, boolean visible) {}
