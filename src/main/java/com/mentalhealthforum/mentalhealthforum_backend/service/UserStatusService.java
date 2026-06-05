package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.userStatus.UserStatusResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserStatusService {
    Mono<UserStatusResponse> getUserStatus(UUID userId, ViewerContext viewerContext);
}
