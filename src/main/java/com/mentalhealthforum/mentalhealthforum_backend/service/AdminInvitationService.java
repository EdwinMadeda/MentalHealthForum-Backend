package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.PendingAdminInviteResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.PendingInviteSortField;
import com.mentalhealthforum.mentalhealthforum_backend.model.AdminInvitationEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface AdminInvitationService {
    Mono<AdminInvitationEntity> createInvitation(KeycloakUserDto keycloakUserDto, String invitedById);

    Mono<PendingAdminInviteResponse> updateInvitation(KeycloakUserDto keycloakUserDto);

    Mono<PendingAdminInviteResponse> syncPendingInviteFromKeycloak(String userId);

    Mono<Void> processVerificationSuccess(String userId);

    Mono<Void> processPasswordResetSuccess(String userId);

    Mono<Void> updateOnboardingStage(String userId, OnboardingStage onboardingStage);


    Mono<PendingAdminInviteResponse> getPendingInvite(String userId);

    Mono<PaginatedResponse<PendingAdminInviteResponse>> getPendingInvites(
            int page,
            int size,
            GroupPath[] groups,
            UUID invitedByUserId,
            String search,
            OnboardingStage onboardingStage,
            PendingInviteSortField sortBy,
            String sortDirection
    );

    Mono<Void> completeInvitation(UUID keycloakId);

    Mono<Void> purgeExpiredInvitation(AdminInvitationEntity invitation);
}
