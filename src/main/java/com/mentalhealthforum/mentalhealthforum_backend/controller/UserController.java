package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.RegisterUserRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.ResetPasswordRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UpdateUserProfileRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InsufficientPermissionException;
import com.mentalhealthforum.mentalhealthforum_backend.service.AppUserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserActivityService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.AppUserServiceImpl;
import com.mentalhealthforum.mentalhealthforum_backend.utils.DateTimeUtils;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AppUserService appUserService;
    private final UserActivityService userActivityService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public UserController(UserService userService, AppUserServiceImpl appUserService, UserActivityService userActivityService, JwtClaimsExtractor jwtClaimsExtractor) {
        this.userService = userService;
        this.appUserService = appUserService;
        this.userActivityService = userActivityService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // -------------------------------------------------------------------------
    // USER MANAGEMENT (Refactored to Reactive)
    // -------------------------------------------------------------------------

    @PostMapping("/register")
        public Mono<ResponseEntity<StandardSuccessResponse<String>>> registerUser(
            @Valid @RequestBody RegisterUserRequest registerUserRequest) {

        return userService.createUserInStaging(registerUserRequest)
                .map(email -> {
                    String message = "Registration request accepted. Please check your email to complete activation.";
                    return ResponseEntity.accepted().body(
                            new StandardSuccessResponse<>(message, email)
                    );
                });
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<StandardSuccessResponse<UserResponse>>> getUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId
    ) {
        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        Mono<Void> syncMono = userService.getUser(String.valueOf(userId))
                .flatMap(keycloakUserDto -> appUserService.syncUserViaAdminClient(keycloakUserDto, viewerContext))
                .then();

        return syncMono
                .then(appUserService.getAppUserWithContext(String.valueOf(userId), viewerContext))
                .map(user -> {
                    String message = "User details retrieved successfully";
                    StandardSuccessResponse<UserResponse> response = new StandardSuccessResponse<>(message, user);
                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<UserResponse>>>> getActiveUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true", name = "current_user_first") @Parameter(name = "current_user_first") boolean currentUserFirst,
            @RequestParam(required = false, name = "is_connected") @Parameter(name = "is_connected", description = "Filter by connection status: true (connected), false (not connected)") Boolean isConnected,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String[] groups,
            @RequestParam(required = false, name = "search") @Parameter(name = "search", description = "Search display_name (case-insensitive contains)") String search,
            @RequestParam(defaultValue = "display_name", name = "sort_by") @Parameter(name = "sort_by", description = "Field to sort by: display_name, date_joined, posts_count, reputation_score, last_posted_at, last_active_at") String sortBy,
            @RequestParam(required = false, name = "sort_direction") @Parameter(name = "sort_direction", description = "Sort direction: asc or desc") String sortDirection
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // userService.getAllUsers returns Mono<PaginatedResponse<UserRepresentation>>
        return appUserService.getActiveAppUsersWithContext(page, size, currentUserFirst, isConnected, role, groups, search, sortBy, sortDirection, viewerContext)
                .map(paginatedUsers -> {
                    String message = "User records retrieved successfully.";
                    StandardSuccessResponse<PaginatedResponse<UserResponse>> response = new StandardSuccessResponse<>(message, paginatedUsers);
                    return ResponseEntity.ok(response);
                });
    }

    @PatchMapping("/{userId}")
    public Mono<ResponseEntity<StandardSuccessResponse<UserResponse>>> updateUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // --- Authorization check ---
        if(viewerContext == null || !viewerContext.getUserId().equals(String.valueOf(userId))){
            throw new InsufficientPermissionException("Forbidden: Cannot update another user's profile.");
        }

        // Try Keycloak update first
            return userService.updateUserProfile(String.valueOf(userId), updateUserProfileRequest)
                .flatMap(profileUpdateResult -> {
                    // Keycloak succeeded, now try local DB
                    return appUserService.updateLocalProfile(String.valueOf(userId), viewerContext, updateUserProfileRequest);
                })
                .map(updatedUser -> {

                    String message = "Profile updated successfully.";
                    if (updatedUser.getPendingEmail() != null) {
                        message = String.format(
                                "Profile updated. A verification link has been sent to %s. " +
                                        "Your email will update once verified.",
                                updateUserProfileRequest.email().toLowerCase()
                        );
                    }

                    return ResponseEntity.ok(new StandardSuccessResponse<>(message, updatedUser));
                });
    }


    @PostMapping("/reset-password")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> resetPassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String userId,
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // --- Authorization check ---
        if(viewerContext == null || !viewerContext.getUserId().equals(userId)){
            throw new InsufficientPermissionException("Forbidden: Cannot reset another user's password.");
        }

        // userService.resetPassword returns Mono<Void>. We use then() to wait for completion.
        return userService.resetPassword(userId, resetPasswordRequest)
                .then(Mono.fromCallable(() -> {
                    String message = "Password reset successfully.";
                    StandardSuccessResponse<Void> response = new StandardSuccessResponse<>(message);
                    return ResponseEntity.ok(response);
                }));
    }

    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> softDeleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // --- Authorization check ---
        if(viewerContext == null || !viewerContext.getUserId().equals(String.valueOf(userId))){
            throw new InsufficientPermissionException("Forbidden: Cannot delete another user's profile.");
        }

        Instant scheduledAt = Instant.now().plus(AppConstants.ACCOUNT_DELETION_RETENTION_WINDOW);
        String readableDate = DateTimeUtils.toHumanReadable(
                scheduledAt,
                String.format("within %s days", AppConstants.ACCOUNT_DELETION_RETENTION_WINDOW)
        );

        return userService.softDeleteUser(String.valueOf(userId))
                .thenReturn(ResponseEntity.ok(
                        new StandardSuccessResponse<>(
                               String.format(
                                       "Account deletion scheduled. You have until %s to cancel. " +
                                               "Your data will be permanently removed after that period.",
                                       readableDate
                               )
                        )
                ));
    }

    @PostMapping("/{userId}/reactivate")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> reactivateAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // -- Authorization check --
        if(viewerContext == null || !viewerContext.getUserId().equals(String.valueOf(userId))){
            throw new InsufficientPermissionException("Forbidden: Cannot reactivate another's account");
        }

        return userActivityService.reactivateUser(userId)
                .thenReturn(ResponseEntity.ok(
                        new StandardSuccessResponse<>("Account reactivated successfully. Welcome back!")
                ));

    }
}