package com.mentalhealthforum.mentalhealthforum_backend.controller.admin;

import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.service.*;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.AccountPurgeSchedulerService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private static final String MANUAL_FALLBACK_MESSAGE = "Please provide the temporary credentials below to the user manually.";

    private final AdminUserService adminUserService;
    private final AppUserService appUserService;
    private final AdminInvitationService adminInvitationService;
    private final AccountPurgeSchedulerService accountPurgeSchedulerService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public AdminUserController(
            AdminUserService adminUserService,
            AppUserService appUserService,
            AdminInvitationService adminInvitationService,
            AccountPurgeSchedulerService accountPurgeSchedulerService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.adminUserService = adminUserService;
        this.appUserService = appUserService;
        this.adminInvitationService = adminInvitationService;
        this.accountPurgeSchedulerService = accountPurgeSchedulerService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    /**
     * ADMIN-ONLY: Creates a new user with admin privileges.

     * Different from self-registration:
     * - Auto-generates password (returns in response)
     * - Sets pending actions for onboarding
     * - Allows explicit group assignment
     * - Returns invitation details for manual sending

     * Security: Requires ADMIN role or equivalent privilege.
     *
     * @param jwt Authentication token for authorization check
     * @param request User creation details including group assignment
     * @return Created user response with temporary credentials
     */
    @PostMapping("/create")
    public Mono<ResponseEntity<StandardSuccessResponse<AdminCreateUserResponse>>> createUserAsAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreateUserRequest request){

        log.info("Admin creating user: email = {}, group = {}", request.email(), request.group().getPath());

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        return adminUserService.createUserAsAdmin(request, viewerContext)
                .map(response -> {
                    // Build success response
                    String message = "User created successfully. %s".formatted(
                            response.emailSent()
                                ? "An invitation email with temporary credentials has been sent to the user."
                                : request.sendInvitationEmail()
                                    ? "The invitation email failed to send. " + MANUAL_FALLBACK_MESSAGE
                                    : MANUAL_FALLBACK_MESSAGE);

                    StandardSuccessResponse<AdminCreateUserResponse> successResponse =
                            new StandardSuccessResponse<>(message, response);

                    // Return 201 Created with response body
                    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
                });
    }

    @PostMapping("/{userId}/reissue-invite")
    public Mono<ResponseEntity<StandardSuccessResponse<AdminCreateUserResponse>>> reissueInvitation(
            @PathVariable UUID userId,
            @Valid @RequestBody ReissueInvitationRequest reissueInvitationRequest){
        return adminUserService.reissueAdminInvitation(String.valueOf(userId), reissueInvitationRequest)
                .map( response -> {
                    String message = "Invitation reissued successfully. %s".formatted(
                            response.emailSent()
                                    ? "An Invitation email with temporary credentials has been sent to the user."
                                    : reissueInvitationRequest.sendInvitationEmail() ?
                                      "The invitation email failed to send. " + MANUAL_FALLBACK_MESSAGE
                                      : MANUAL_FALLBACK_MESSAGE);

                    StandardSuccessResponse<AdminCreateUserResponse> successResponse =
                            new StandardSuccessResponse<>(message, response);

                    return ResponseEntity.ok(successResponse);
                });
    }

    @GetMapping("/pending-invites")
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<PendingAdminInviteDto>>>> getPendingInvites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String[] groups,
            @RequestParam(required = false, name = "invited_by_user_id") @Parameter(name = "invited_by_user_id") UUID invitedByUserId,
            @RequestParam(required = false, name = "onboarding_stage") @Parameter(description = "Filter by stage: AWAITING_VERIFICATION, AWAITING_PASSWORD_RESET, AWAITING_PROFILE_COMPLETION") OnboardingStage onboardingStage,
            @RequestParam(required = false, name = "search") @Parameter(name = "search") String search,
            @RequestParam(defaultValue = "date_created", name = "sort_by") @Parameter(name = "sort_by", description = "Field to sort by: username, email, date_created") String sortBy,
            @RequestParam(required = false, name = "sort_direction") @Parameter(name = "sort_direction", description = "Sort direction: asc or desc") String sortDirection
       ){

        return  adminInvitationService.getPendingInvites(page, size, groups, invitedByUserId, search, onboardingStage, sortBy, sortDirection)
                .map(paginated -> ResponseEntity.ok(
                        new StandardSuccessResponse<>("Pending invitations retrieved.", paginated)
                ));
    }

    @PostMapping("/{userId}")
    public Mono<ResponseEntity<StandardSuccessResponse<UserResponse>>> updateUserAsAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest adminUpdateUserRequest){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return adminUserService.updateUserAsAdmin(String.valueOf(userId), adminUpdateUserRequest)
                .flatMap(keycloakUserDto -> appUserService.syncUserViaAdminClient(keycloakUserDto, viewerContext)
                .map(user -> {
                    String message = "User details updated successfully";
                    StandardSuccessResponse<UserResponse> response = new StandardSuccessResponse<>(message, user);
                    return ResponseEntity.ok(response);
                }));
    }

    @DeleteMapping("/invites/{userId}")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> revokeInvitation(
            @PathVariable UUID userId){
            log.info("Admin revoking invitation for user ID: {}", userId);

            return adminUserService.revokeInvitation(String.valueOf(userId))
                    .thenReturn(ResponseEntity.ok(
                            new StandardSuccessResponse<>("Invitation revoked and user deleted successfully.", null)
                    ));
    }

    @PostMapping("/purge-expired")
    public Mono<ResponseEntity<StandardSuccessResponse<Integer>>> purgeExpiredAccounts(){
        return accountPurgeSchedulerService.purgeExpiredAccounts()
                .map(count  -> ResponseEntity.ok(
                        new StandardSuccessResponse<>(
                                String.format("Purge for %s expired accounts triggered successfully. ", count)
                        )
                ));
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<UserResponse>>>> getAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true", name = "current_user_first") @Parameter(name = "current_user_first") boolean currentUserFirst,
            @RequestParam(required = false, name = "is_active") @Parameter(name = "is_active") Boolean isActive,
            @RequestParam(required = false, name = "is_connected") @Parameter(name = "is_connected", description = "Filter by connection status: true (connected), false (not connected)") Boolean isConnected,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String[] groups,
            @RequestParam(required = false, name = "search") @Parameter(name = "search", description = "Search display_name (case-insensitive contains)") String search,
            @RequestParam(defaultValue = "display_name", name = "sort_by") @Parameter(name = "sort_by", description = "Field to sort by: display_name, date_joined, posts_count, reputation_score, last_posted_at, last_active_at") String sortBy,
            @RequestParam(required = false, name = "sort_direction") @Parameter(name = "sort_direction", description = "Sort direction: asc or desc") String sortDirection
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);

        // userService.getAllUsers returns Mono<PaginatedResponse<UserRepresentation>>
        return appUserService.getAllAppUsersWithContext(page, size, currentUserFirst, isActive, isConnected, role, groups, search, sortBy, sortDirection, viewerContext)
                .map(paginatedUsers -> {
                    String message = "User records retrieved successfully.";
                    StandardSuccessResponse<PaginatedResponse<UserResponse>> response = new StandardSuccessResponse<>(message, paginatedUsers);
                    return ResponseEntity.ok(response);
                });
    }
}
