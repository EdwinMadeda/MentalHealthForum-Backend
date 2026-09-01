package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import reactor.core.publisher.Mono;

public interface AdminUserService {
    Mono<AdminCreateUserResponse> createUserAsAdmin(AdminCreateUserRequest request, ViewerContext viewerContext);

    Mono<AdminCreateUserResponse> reissueAdminInvitation(String userId, ReissueInvitationRequest request);

    Mono<PendingAdminInviteDto> updatePendingAdminInvite(String userId, UpdatePendingAdminInviteRequest request);

    Mono<KeycloakUserDto> updateUserAsAdmin(String userId, AdminUpdateUserRequest request);

    Mono<Void> revokeInvitation(String userId);
}
