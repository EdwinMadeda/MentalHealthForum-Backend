package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface AdminUserService {

    Mono<OperationResponse<AdminCreateUserResponse>> createUserAsAdmin(AdminCreateUserRequest request, ViewerContext viewerContext);

    Mono<OperationResponse<AdminCreateUserResponse>> reissueAdminInvitation(String userId, ReissueInvitationRequest request, ViewerContext viewerContext);

    Mono<OperationResponse<PendingAdminInviteDto>> updatePendingAdminInvite(String userId, UpdatePendingAdminInviteRequest request, ViewerContext viewerContext);

    Mono<OperationResponse<UserResponse>> updateUserAsAdmin(String userId, AdminUpdateUserRequest request, ViewerContext viewerContext);

    Mono<Void> revokeInvitation(String userId, ViewerContext viewerContext);

    Mono<List<AvailableGroup>> getAvailableGroups(GroupContext context, String userId, ViewerContext viewerContext);

    Mono<AdminUserDetailsDto<UserResponse>> getAdminUserDetails(String s, ViewerContext viewerContext);

    Mono<AdminUserDetailsDto<PendingAdminInviteDto>> getPendingInviteDetails(String s, ViewerContext viewerContext);
}
