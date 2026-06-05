package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.userStatus.UserStatusResponse;
import com.mentalhealthforum.mentalhealthforum_backend.service.PendingActionsService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserModerationService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserStatusService;
import com.mentalhealthforum.mentalhealthforum_backend.utils.DateTimeUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class UserStatusServiceImpl implements UserStatusService {

    private final UserModerationService userModerationService;
    private final PendingActionsService pendingActionsService;

    public UserStatusServiceImpl(
            UserModerationService userModerationService,
            PendingActionsService pendingActionsService) {
        this.userModerationService = userModerationService;
        this.pendingActionsService = pendingActionsService;
    }

    @Override
    public Mono<UserStatusResponse> getUserStatus(UUID userId, ViewerContext viewerContext){

        return userModerationService.isUserBanned(userId)
                .zipWith(userModerationService.isUserSuspended(userId))
                .zipWith(userModerationService.isUserMuted(userId))
                .zipWith(pendingActionsService.hasPendingActions(String.valueOf(userId)))
                .flatMap(tuple -> {
                    boolean isBanned = tuple.getT1().getT1().getT1();
                    boolean isSuspended = tuple.getT1().getT1().getT2();
                    boolean isMuted = tuple.getT1().getT2();
                    boolean hasPendingActions = tuple.getT2();
                    boolean isOnboarding = viewerContext.isOnboarding();


                    //Ban has highest priority
                    if(isBanned){
                        // Fetch ban details for the message
                        return userModerationService.getActiveBanForUser(userId, viewerContext)
                                .map(ban -> UserStatusResponse.builder()
                                        .canAccess(false)
                                        .message(String.format(
                                                "Your account has been permanently banned. Reason: %s. Please contact support.",
                                                ban.getReason()
                                        ))
                                        .restriction(UserStatusResponse.RestrictionDetails.builder()
                                                .type("BANNED")
                                                .reason(ban.getReason())
                                                .imposedBy(ban.getImposedByDisplayName())
                                                .build())
                                        .build());
                    }

                    // Suspension has next priority
                    if(isSuspended){
                        // Fetch suspension details for the message
                        return userModerationService.getActiveSuspendForUser(userId, viewerContext)
                                .map(suspension -> {
                                    String expiry = DateTimeUtils.toHumanReadable(suspension.getExpiresAt(), "indefinitely");

                                    return UserStatusResponse.builder()
                                            .canAccess(false)
                                            .message(String.format(
                                                    "Your account is suspended until %s. Reason: %s.",
                                                    expiry,
                                                    suspension.getReason()
                                            ))
                                            .restriction(UserStatusResponse.RestrictionDetails.builder()
                                                    .type("SUSPENDED")
                                                    .reason(suspension.getReason())
                                                    .imposedBy(suspension.getImposedByDisplayName())
                                                    .build())
                                            .build();
                                });
                    }

                    // Onboarding
                    if(isOnboarding){
                        return Mono.just(UserStatusResponse.builder()
                                .canAccess(false)
                                .message("Please complete your profile setup to access the forum.")
                                .redirectUrl("/onboarding")
                                .restriction(UserStatusResponse.RestrictionDetails.builder()
                                        .type("ONBOARDING")
                                        .build())
                                .build());
                    }

                    // Pending actions
                    if(hasPendingActions){
                        return Mono.just(UserStatusResponse.builder()
                                .canAccess(false)
                                .message("Please complete the required actions to access the forum.")
                                .redirectUrl("/auth/pending-actions")
                                .restriction(UserStatusResponse.RestrictionDetails.builder()
                                        .type("PENDING_ACTIONS")
                                        .build())
                                .build());
                    }

                    if(isMuted){
                        // Fetch mute details for the message
                        return userModerationService.getActiveMuteForUser(userId, viewerContext)
                                .map(mute -> {
                                    String expiry = DateTimeUtils.toHumanReadable(mute.getExpiresAt(), "indefinitely");

                                    return UserStatusResponse.builder()
                                            .canAccess(true)
                                            .message(String.format(
                                                    "You are currently muted. You can browse but cannot create new posts or threads until %s. Reason: %s.",
                                                    expiry,
                                                    mute.getReason()
                                            ))
                                            .restriction(UserStatusResponse.RestrictionDetails.builder()
                                                    .type("MUTED")
                                                    .reason(mute.getReason())
                                                    .imposedBy(mute.getImposedByDisplayName())
                                                    .build())
                                            .build();
                                });
                    }

                    // All good
                    return Mono.just(UserStatusResponse.builder()
                            .canAccess(true)
                            .message("Your account is in good standing.")
                            .build());
                });
    }


}
