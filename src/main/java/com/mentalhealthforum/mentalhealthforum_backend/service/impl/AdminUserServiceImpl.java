package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.AdminInvitePayload;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.KeycloakUserDto;
import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.*;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AdminInvitationRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.mentalhealthforum.mentalhealthforum_backend.utils.ChangeUtils.*;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.PatchUtils.*;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.NormalizeUtils.normalizeUnicode;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final KeycloakAdminManager adminManager;
    private final KeycloakUserDtoMapper keycloakUserDtoMapper;
    private final VerificationService verificationService;
    private final NovuService novuService;
    private final AdminInvitationService adminInvitationService;
    private final AdminInvitationRepository adminInvitationRepository;
    private final AppUserRepository appUserRepository;


    // 1. Define the internal "Assembly Line" package
    private record AdminUserContext(
            String userId,
            String username,
            String email,
            String firstName,
            String tempPassword,
            String groupPath,
            boolean sendInvitationEmail
    ){}

    public AdminUserServiceImpl(
            KeycloakAdminManager adminManager,
            KeycloakUserDtoMapper keycloakUserDtoMapper,
            VerificationService verificationService,
            NovuService novuService,
            AdminInvitationService adminInvitationService,
            AdminInvitationRepository adminInvitationRepository,
            AppUserRepository appUserRepository) {
        this.adminManager = adminManager;
        this.keycloakUserDtoMapper = keycloakUserDtoMapper;
        this.verificationService = verificationService;
        this.novuService = novuService;
        this.adminInvitationService = adminInvitationService;
        this.adminInvitationRepository = adminInvitationRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Creates a new user as an administrator.
     * -
     * Different from self-registration:
     * - Auto-generates password (admin doesn't know user's password)
     * - Sets pending actions for onboarding (email verification, password reset, etc.)
     * - Allows explicit group assignment (not default /members/new)
     * - Optionally sends invitation email
     *
     * @param request       Admin user creation request containing user details and group assignment
     * @param viewerContext Viewer Context
     * @return Mono containing the AdminCreateUserResponse with user ID, temporary password, and invitation details
     */
    @Override
    public Mono<AdminCreateUserResponse> createUserAsAdmin(
            AdminCreateUserRequest request,
            ViewerContext viewerContext) {
        return Mono.fromCallable(()-> {
                    // 1. Normalize and validate inputs
                    String email = request.email().trim().toLowerCase();
                    String firstName = request.firstName().trim();
                    String lastName = request.lastName().trim();

                    String username = request.username() != null?
                            request.username().trim() :
                            generateUsername(firstName, lastName);

                    String temporaryPassword = generateTemporaryPassword();

                    // Validate uniqueness (same as self-registration)
                    if(adminManager.findUserByEmail(email).isPresent()){
                        throw new UserExistsException("An account already exists for this email.");
                    }

                    if(adminManager.findUserByUsername(username).isPresent()){
                        // If auto-generated username exists, add random suffix
                        username = "%s.%d".formatted(username, ThreadLocalRandom.current().nextInt(100, 199));
                    }

                    // Superadmin-only group restrictions
                    validateGroupAssignmentPermission(request.group(), viewerContext);

                    var passwordCred = adminManager.createPasswordCredential(temporaryPassword);

                    // Create user with PENDING ACTIONS
                    UserRepresentation userRep = new UserRepresentation();
                    userRep.setEnabled(true);
                    userRep.setUsername(username);
                    userRep.setEmail(email);
                    userRep.setFirstName(firstName);
                    userRep.setLastName(lastName);
                    userRep.setCredentials(List.of(passwordCred));

                    // Set pending actions
                    userRep.setEmailVerified(false); // Admin-created users need to verify
                    userRep.setRequiredActions(determineRequiredActions(request.group()));

                    // Create user in Keycloak
                    String userId = adminManager.createUser(userRep);

                    // Assign to specified group (not default /members/new)
                    adminManager.assignUserToGroup(userId, request.group());
                    adminManager.assignInternalRole(userId, InternalRole.ONBOARDING);

                    // Wrap in the Record instead of a Map
                    return new AdminUserContext(
                            userId,
                            username,
                            email,
                            firstName,
                            temporaryPassword,
                            request.group().getPath(),
                            request.sendInvitationEmail()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                // --- SURGICAL INJECTION START ---
                .flatMap(ctx -> {
                    return Mono.fromCallable(() -> adminManager.findUserByUserId(ctx.userId)
                            .map(keycloakUserDtoMapper::mapToKeycloakUserDto)
                            .orElseThrow(() -> new UserDoesNotExistException("Failed to retrieve created user")))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(keycloakUserDto ->
                                    adminInvitationService.createInvitation(keycloakUserDto, viewerContext.getUserId()))
                            .thenReturn(ctx); // Return the context to keep the assembly line moving
                })
                .flatMap(this::handleInvitationFlow);
    }

    @Override
    public Mono<AdminCreateUserResponse> reissueAdminInvitation(String userId, ReissueInvitationRequest request, ViewerContext viewerContext){
        UUID userUUID = UUID.fromString(userId);
        String email = request.email().trim().toLowerCase();

        return Mono.fromCallable(()-> {
                    // 1. Find user in Keycloak (Blocking call)
                    UserRepresentation userRep = adminManager.findUserByUserId(userId)
                            .orElseThrow(UserDoesNotExistException::new);

                    return userRep;
                }).subscribeOn(Schedulers.boundedElastic())
                .zipWith(appUserRepository.existsByKeycloakId(userUUID))
                .flatMap(tuple -> {
                    UserRepresentation userRep = tuple.getT1();
                    boolean inAppUsers = tuple.getT2();

                    // 2. Security Check: Don't resend if they are already synced (active)
                    if(inAppUsers){
                        throw new UserAlreadyActiveException();
                    }

                    // 3. Security Check: Don't resend if they are already verified
                    if(Boolean.TRUE.equals(userRep.isEmailVerified())){
                        throw new InvitationAlreadyVerifiedException();
                    }

                    return validateTargetUserModification(userId, viewerContext, "reissue invite for")
                            .thenReturn(userRep);
                })
                .flatMap(userRep -> Mono.fromCallable(() -> {

                    // Process is email change
                    boolean isEmailChanged = setIfChangedStrict(
                            email,
                            userRep.getEmail(),
                            userRep::setEmail
                    );

                    if(request.group() != null){
                        // Superadmin-only group restrictions
                        validateGroupAssignmentPermission(request.group(), viewerContext);
                    }

                    if(isEmailChanged) {
                        adminManager.updateUser(userRep);
                    }

                    // 4. Generate a fresh temporary password
                    String newTempPassword = generateTemporaryPassword();

                    // 5. Update Keycloak (Resetting the temp password)
                    adminManager.resetPassword(userRep.getId(), newTempPassword);

                    // Note: We'd need to know the groupPath. If we don't store it,
                    // we can fetch the user's current groups from Keycloak.
                    // Fetch group path while still in the blocking thread pool
                    if(request.group() != null){
                        adminManager.assignUserToGroup(userId, request.group());
                    }

                    String groupPath = adminManager.getUserPrimaryGroupPath(userRep.getId());

                    // Wrap in the Record instead of a Map
                    return new AdminUserContext(
                            userRep.getId(),
                            userRep.getUsername(),
                            userRep.getEmail(),
                            userRep.getFirstName(),
                            newTempPassword,
                            groupPath,
                            request.sendInvitationEmail()
                    );

                }).subscribeOn(Schedulers.boundedElastic()))
                // --- SURGICAL INJECTION START ---
                .flatMap(ctx ->{
                    return Mono.fromCallable(() -> adminManager.findUserByUserId(ctx.userId)
                                    .map(keycloakUserDtoMapper::mapToKeycloakUserDto)
                                    .orElseThrow(() -> new UserDoesNotExistException("Failed to retrieve user")))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(adminInvitationService::updateInvitation)
                            .thenReturn(ctx); // Return the co
                })
                .flatMap(ctx ->
                        adminInvitationRepository.findByKeycloakId(UUID.fromString(ctx.userId))
                                .flatMap(adminInvitation -> {
                                    // RESET the flags so the new temp password actually works
                                    adminInvitation.setIsInitialLogin(true);
                                    adminInvitation.setCurrentStage(OnboardingStage.AWAITING_VERIFICATION);
                                    return adminInvitationRepository.save(adminInvitation);
                                })
                                .thenReturn(ctx))
                .flatMap(this::handleInvitationFlow);
    }

    @Override
    public Mono<PendingAdminInviteDto> updatePendingAdminInvite(String userId, UpdatePendingAdminInviteRequest request, ViewerContext viewerContext){
        UUID userUUID = UUID.fromString(userId);

        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers -> {
                    // If user is already synced we shouldn't use this endpoint
                    if (inAppUsers) {
                        return Mono.error(
                                new ApiException("User is already synced: Use 'Update Synced User' instead",
                                        ErrorCode.VALIDATION_FAILED
                                ));

                    }

                    // Check if they're in the lobby
                    return adminInvitationRepository.findByKeycloakId(userUUID)
                            .switchIfEmpty(Mono.error(new UserDoesNotExistException(
                                    "User not found in pending invitations."
                            )))
                            .flatMap(invitation -> {
                                
                                OnboardingStage stage = invitation.getCurrentStage();
                                if(invitation.getCurrentStage() == OnboardingStage.AWAITING_VERIFICATION){
                                    return Mono.error(new UserNotReadyException(
                                            "User hasn't verified email yet. Use 'Reissue Invite' to send a new invitation."
                                    ));
                                }

                                // Proceed with keycloak update
                                return updateUserInKeycloak(userId, request.getGroup(), request.getIsEnabled(), viewerContext);
                            })
                            .flatMap(adminInvitationService::updateInvitation);

                });
    }

    @Override
    public Mono<KeycloakUserDto> updateUserAsAdmin(String userId, AdminUpdateUserRequest request, ViewerContext viewerContext) {
        UUID userUUID = UUID.fromString(userId);

        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers -> {
                    // User might be in the lobby (onboarding not complete)
                    if (!inAppUsers) {
                        // Check if they're in the lobby
                        return adminInvitationRepository.existsByKeycloakId(userUUID)
                                .flatMap(inLobby -> {
                                    if(inLobby){
                                        return Mono.error(new UserNotReadyException(
                                                "Cannot modify profile: User has not completed onboarding. " +
                                                        "Use 'Reissue Invite' to manage lobby users."
                                        ));
                                    }
                                    else {
                                        return Mono.error(new UserDoesNotExistException(
                                                "User not found in the local system. Please ensure they're logged in at least once"
                                        ));
                                    }
                                });
                    }

                    // Proceed with keycloak update
                    return updateUserInKeycloak(userId, request.getGroup(), request.getIsEnabled(), viewerContext);
                });
    }

    @Override
    public Mono<Void> revokeInvitation(String userId) {
        UUID userUUID = UUID.fromString(userId);
        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers ->
                        Mono.fromCallable(()-> {
                                // 1. Fetch the latest state from Keycloak (Blocking call)
                                UserRepresentation userRep = adminManager.findUserByUserId(userId)
                                        .orElseThrow(UserDoesNotExistException::new);

                                // 2. SAFETY GATE: Enforce the "Lobby Only" rule
                                // If they are verified OR Keycloak says they are already synced, they are no longer an "Invitation"

                                // Security Check: Don't revoke if they are already synced (active)
                                if(inAppUsers){
                                    throw new UserAlreadyActiveException();
                                }

                                // Security Check: Don't revoke if they are already verified
                                if(Boolean.TRUE.equals(userRep.isEmailVerified())){
                                    throw new InvitationAlreadyVerifiedException();
                                }

                                adminManager.deleteUser(userId);
                                return userId;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(id -> adminInvitationService.completeInvitation(UUID.fromString(id)))
                );

    }

    /**
     * Reusable logic for the "Invitation" phase of the mpango.
     * This handles the verification link and the conditional Novu trigger.
     */
    private Mono<AdminCreateUserResponse> handleInvitationFlow(AdminUserContext ctx){
        return verificationService.createVerificationLink(ctx.email, VerificationType.INVITED, ctx.groupPath, null)
                .flatMap(invitationLink -> {

                    // Logic: Only send if the admin requested it
                    if(ctx.sendInvitationEmail){
                        // 6. Trigger Novu with the NEW temp password

                        AdminInvitePayload payload = new AdminInvitePayload(
                                ctx.firstName,
                                ctx.tempPassword,
                                invitationLink,
                                ctx.groupPath
                        );

                        return novuService.triggerEvent(NovuWorkflow.ADMIN_ONBOARDING_INVITE, ctx.userId, ctx.email, payload)
                                .map(sentStatus ->  new AdminCreateUserResponse(
                                        ctx.userId,
                                        ctx.username,
                                        ctx.tempPassword,
                                        invitationLink,
                                        sentStatus
                                ));
                    }

                    return Mono.just(new AdminCreateUserResponse(
                            ctx.userId,
                            ctx.username,
                            ctx.tempPassword,
                            invitationLink,
                            false
                    ));
                });
    }

    private String generateUsername(String firstName, String lastName) {
        // Normalize Unicode characters (é → e, ç → c)
        String normalizedFirstName = normalizeUnicode(firstName.toLowerCase());
        String normalizedLastName = normalizeUnicode(lastName.toLowerCase());

        String base = "%s.%s".formatted(normalizedFirstName, normalizedLastName);

        // Remove any remaining invalid characters
        String cleaned = base.replaceAll("[^a-z0-9._]", "");

        // Remove leading/trailing dots and underscores
        cleaned = cleaned.replaceAll("^[._]+|[._]+$", "");

        // Replace multiple consecutive dots/underscores with single
        cleaned = cleaned.replaceAll("[._]{2,}", ".");

        // Ensure valid length
        if(cleaned.isEmpty()){
            throw new UsernameGenerationException(
                    String.format(
                            "Could not generate a valid username from names: '%s %s'. " +
                            "Please provide a username manually.",
                            firstName, lastName
                    )
            );
        }
        else if(cleaned.length() < 3){
            throw new UsernameGenerationException(
                    String.format(
                            "Could not generate username '%s' is too short (minimum 3 characters). " +
                            "Please provide a username manually.",
                            cleaned
                    )
            );
        }

        return cleaned.substring(0, Math.min(cleaned.length(), 30));
    }

    private String generateTemporaryPassword() {
        final int PASSWORD_LENGTH = 12;

        // Character sets that avoid ambiguous characters (no 0, O, I, l, 1, etc.)
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // No I, O
        String lower = "abcdefghijkmnopqrstuvwxyz"; // No l
        String digits = "23456789"; // No 0, 1
        String special = "!@#$%^&*";

        SecureRandom random = new SecureRandom();

        // Build password ensuring at least one of each required type
        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // fill the rest
        String allChars = upper + lower + digits + special;
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // shuffle
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    private List<String> determineRequiredActions(GroupPath group){
        List<RequiredAction> actions = new ArrayList<>();

        actions.add(RequiredAction.VERIFY_EMAIL);

//        if(group == GroupPath.ADMINISTRATORS){
//            actions.add(RequiredAction.CONFIGURE_TOTP);
//        }

        return actions.stream().map(Enum::name).toList();
    }

    private Mono<KeycloakUserDto> updateUserInKeycloak(String userId, JsonNullable<GroupPath> group, JsonNullable<Boolean> isEnabled, ViewerContext viewerContext){
        UUID userUUID = UUID.fromString(userId);

        // Proceed with keycloak update
        return Mono.fromCallable(()-> {
            // Fetch user from Keycloak
            UserRepresentation userRep = adminManager.findUserByUserId(userId)
                    .orElseThrow(UserDoesNotExistException::new);

            return userRep;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(userRep ->
                        validateTargetUserModification(userId, viewerContext, "modify")
                                .flatMap(currentGroups -> Mono.fromCallable(()-> {

                    // Process isEnabled change
                    boolean isEnabledChanged = patchStrict(
                            isEnabled,
                            userRep.isEnabled(),
                            userRep::setEnabled
                    );

                    // Process group change
                    boolean isGroupChanged = false;

                    // Only evaluate group changes if the admin explicitly provided a group in the payload
                    if(group != null && group.isPresent()){
                        GroupPath targetGroupPath = group.get();

                        // Superadmin-only group restrictions
                        validateGroupAssignmentPermission(targetGroupPath, viewerContext);

                        // Assign the group
                        if(targetGroupPath != null){
                            String targetGroupPathStr = targetGroupPath.getPath();

                            isGroupChanged = currentGroups.isEmpty()
                                    || !currentGroups.contains(targetGroupPathStr);

                            if(isGroupChanged){
                                adminManager.assignUserToGroup(userId, targetGroupPath);
                                log.info("Updated group for user {} to {}", userId, targetGroupPathStr);
                            }

                        }

                    }

                    if(isEnabledChanged || isGroupChanged){
                        adminManager.updateUser(userRep);
                        log.info("Successfully updated profile for user ID: {}", userId);
                    } else {
                        log.debug("No profile changes detected for user ID: {}", userId);
                    }

                    return keycloakUserDtoMapper.mapToKeycloakUserDto(userRep);
                })));
    }

    /**
     * Validates that the viewer has permission to modify the target user.
     *
     * Rules:
     * - Only superadmin can modify a user in SUPER_ADMINISTRATORS
     * - Only superadmin can modify a user in ADMINISTRATORS
     * - Regular admins can modify regular users (MEMBERS, MODERATORS, etc.)
     *
     * @param targetUserId The user being modified
     * @param viewerContext The user making the request
     * @param action The action being performed (for error messages)
     * @throws InsufficientPermissionException if the viewer lacks permission
     */
    private Mono<List<String>> validateTargetUserModification(String targetUserId, ViewerContext viewerContext, String action){
        return Mono.fromCallable(() -> {

            // Get the target user's current groups
            List<String> currentGroups = adminManager.getUserGroups(targetUserId);

            // Check if target user is a superadmin
            boolean isTargetSuperAdmin = currentGroups.stream().anyMatch(
                    groupPath -> GroupPath.SUPER_ADMINISTRATORS.getPath().equals(groupPath)
            );

            boolean isTargetAdmin = currentGroups.stream().anyMatch(
                    groupPath -> GroupPath.ADMINISTRATORS.getPath().equals(groupPath) ||
                            GroupPath.SUPER_ADMINISTRATORS.getPath().equals(groupPath)
            );

            // Regular admin cannot modify superadmin
            if(isTargetSuperAdmin && !viewerContext.isSuperAdmin()){
                throw new InsufficientPermissionException(
                      String.format( "Only superadmin can %s a superadmin user", action)
                );
            }

            // Regular admin cannot modify admin
            if(isTargetAdmin && !viewerContext.isAdmin()){
                throw new InsufficientPermissionException(
                       String.format("Only superadmin can %s an admin user", action)
                );
            }

            return currentGroups;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Validates that the viewer has permission to assign a user to the specified group.
     * Rules:
     * - Only superadmins can assign users to SUPER_ADMINISTRATORS group
     * - Only superadmins can assign users to ADMINISTRATORS group
     * - Any admin can assign users to MEMBERS, MODERATORS, etc.
     *
     * @param targetGroupPath The group being assigned
     * @param viewerContext The user making the request
     * @throws InsufficientPermissionException if the viewer lacks permission
     */
    private void validateGroupAssignmentPermission(GroupPath targetGroupPath, ViewerContext viewerContext){
        if(!GroupPath.isSuperAdminOnlyGroup(targetGroupPath, viewerContext)){
            throw new InsufficientPermissionException(
                    String.format("Only superadmins can assign the '%s' group.",
                            targetGroupPath.getDisplayName())
            );
        }
    }

}
