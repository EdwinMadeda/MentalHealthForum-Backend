package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.onboarding.OnboardingPolicy;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.onboarding.OnboardingStatusResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UpdateUserProfileRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InsufficientPermissionException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.OnboardingPolicyViolationException;
import com.mentalhealthforum.mentalhealthforum_backend.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final UserService userService;
    private final AppUserService appUserService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public OnboardingController(
            OnboardingService onboardingService,
            UserService userService,
            AppUserService appUserService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.onboardingService = onboardingService;
        this.userService = userService;
        this.appUserService = appUserService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }


    @GetMapping("/status")
    public Mono<ResponseEntity<StandardSuccessResponse<OnboardingStatusResponse>>> getOnboardingStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        String userId = viewerContext.getUserId();

        return userService.getUser(userId)
                .flatMap(keycloakUserDto ->
                        appUserService.syncUserViaAdminClient(keycloakUserDto, viewerContext))
                .then(onboardingService.getOnboardingStatus(viewerContext)
                        .map(onboardingStatusResponse -> {
                            var success = new StandardSuccessResponse<>(
                                    "Onboarding status retrieved successfully.",
                                    onboardingStatusResponse
                            );
                            return ResponseEntity.ok(success);
                        }));
    }


    @PatchMapping("/complete")
    public Mono<ResponseEntity<StandardSuccessResponse<UserResponse>>> completeOnboarding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        String userId = viewerContext.getUserId();

        // Try Keycloak update first
        return Mono.just(updateUserProfileRequest)
                .flatMap(updateUserOnboardingProfileRequest -> {
                    OnboardingPolicy.Result result = viewerContext.checkOnboardingPolicy(updateUserOnboardingProfileRequest);

                    if(!result.isSatisfied()){
                        return Mono.error(new OnboardingPolicyViolationException(result.violations()));
                    }
                    return userService.updateUserProfile(String.valueOf(userId), updateUserProfileRequest);
                })
                .flatMap(profileUpdateResult -> {
                    // Keycloak succeeded, now try local DB
                    return appUserService.updateLocalProfile(String.valueOf(userId), viewerContext, updateUserProfileRequest)
                            .map(updatedUser -> {

                                String message;
                                if(profileUpdateResult.pendingEmail() != null){
                                    if(profileUpdateResult.emailSent()){
                                        message = String.format(
                                                "Onboarding complete. A verification link has been sent to %s. " +
                                                 "Your email will update once verified.",
                                                profileUpdateResult.pendingEmail()
                                        );
                                    }
                                    else {
                                        message = String.format(
                                                "Onboarding complete, but we're having trouble sending the verification link to %s. " +
                                                "You can request a new link from your profile settings.",
                                                profileUpdateResult.pendingEmail()
                                        );
                                    }
                                }
                                else {
                                    message = "Onboarding completed successfully.";
                                }

                                return ResponseEntity.ok(new StandardSuccessResponse<>(message, updatedUser));
                            });
                });

    }

}
