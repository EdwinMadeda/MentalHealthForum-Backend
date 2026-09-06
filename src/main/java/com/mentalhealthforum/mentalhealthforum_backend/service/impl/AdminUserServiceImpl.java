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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.mentalhealthforum.mentalhealthforum_backend.utils.ChangeUtils.*;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.PatchUtils.*;
import static com.mentalhealthforum.mentalhealthforum_backend.utils.NormalizeUtils.normalizeUnicode;

/**
 * Administrative user management service implementing the complete security model.

 * ============================================================
 * SYSTEM RULES (Enforced by this service)
 * ============================================================

 * 1. HIERARCHY & STATE TRANSITIONS (SYNCED USERS ONLY)
 *    1.1 Canonical Chain: MEMBERS_NEW → MEMBERS_ACTIVE → MEMBERS_TRUSTED →
 *                          MODERATORS_PEER → MODERATORS_PROFESSIONAL →
 *                          ADMINISTRATORS → SUPER_ADMINISTRATORS
 *    1.2 Single-Step Only: Promotions AND demotions must move exactly one level at a time.
 *    1.3 Professional Exception: Direct assignment to MODERATORS_PROFESSIONAL is allowed
 *        from ANY lower tier (MEMBERS_NEW, MEMBERS_ACTIVE, MEMBERS_TRUSTED, or MODERATORS_PEER).
 *        This allows organizations to quickly onboard professional moderators.

 * 2. SELF-MODIFICATION RESTRICTIONS (SYNCED USERS ONLY)
 *    2.1 Superadmins: Cannot promote or disable themselves. Can demote themselves
 *        (subject to system integrity rule 4.1 — last superadmin cannot be removed).
 *    2.2 Regular Admins: Cannot promote or disable themselves. Can demote themselves.
 *    2.3 Moderators: Self-modification rules deferred. (currently retain default capabilities).

 * 3. PEER PROTECTION (SYNCED USERS ONLY)
 *    3.1 Superadmins cannot modify other superadmins (self-demotion is exempt).
 *    3.2 Admins cannot modify other admins (self-demotion is exempt).
 *    3.3 Moderators: Peer modification limits deferred (currently lack admin permissions).

 * 4. SYSTEM INTEGRITY (SYNCED USERS ONLY)
 *    4.1 At least one active superadmin must exist at all times.
 *    4.2 Last remaining superadmin becomes completely immutable — cannot be demoted
 *        (even by self-demotion) or disabled. This is enforced by the last-user safeguard.

 * 5. CREATION & INVITE MANAGEMENT (PENDING USERS ONLY)
 *    5.1 New users only: MEMBERS_NEW or MODERATORS_PROFESSIONAL.
 *    5.2 Reissue/Update Pending: Can freely move between MEMBERS_NEW and MODERATORS_PROFESSIONAL.
 *    5.3 Pending users cannot be assigned administrative tiers.
 *    5.4 Correction: Admin/Superadmin can correct improperly assigned admin tiers back to allowed pending groups.

 * 6. DEFERRED BEHAVIORS
 *    6.1 First-user auto-promotion to superadmin is deferred.

 * 7. GROUP ASSIGNMENT PERMISSIONS
 *    7.1 Only superadmins can directly assign MODERATORS_PROFESSIONAL during creation, reissue,
 *        or pending updates.
 *    7.2 Regular admins CAN promote synced users to MODERATORS_PROFESSIONAL following
 *        Rule 1.2 (one level up from MODERATORS_PEER).
 *    7.3 Only superadmins can assign ADMINISTRATORS or SUPER_ADMINISTRATORS.

 * ============================================================
 * RULE MAPPINGS BY OPERATION
 * ============================================================

 * CREATE USER           → Rules 5.1, 7.1
 * REISSUE INVITATION    → Rules 5.2, 5.3, 5.4, 7.1
 * UPDATE PENDING INVITE → Rules 5.3, 5.4, 7.1
 * UPDATE SYNCED USER    → Rules 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 7.2, 7.3
 * REVOKE INVITATION     → State checks only

 * ============================================================
 * VALIDATION FLOW
 * ============================================================
 * All modifications go through validateTargetUserModification() which enforces:
 *   1. Self-modification checks (Rules 2.1, 2.2) — self-promotion blocked,
 *      self-demotion allowed, self-disable blocked
 *   2. Same-level protection (Rules 3.1, 3.2)
 *   3. Superadmin protection
 *   4. Hierarchy progression (Rules 1.1, 1.2, 1.3)
 *   5. Group assignment permissions (Rules 7.2, 7.3)
 *   6. System integrity (Rules 4.1, 4.2) — last superadmin safeguard
 */

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

    // Internal context carrier for the admin user assembly line.
    // Used to pass data through the reactive pipeline without losing context.
    private record AdminUserContext(
            String userId,
            String username,
            String email,
            String firstName,
            String tempPassword,
            String groupPath,
            boolean sendInvitationEmail
    ) {}

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

     * Different from self-registration:
     * - Auto-generates password (admin doesn't know user's password)
     * - Sets pending actions for onboarding (email verification, password reset, etc.)
     * - Allows explicit group assignment (not default /members/new)
     * - Optionally sends invitation email
     *
     * <p><b>Rules Applied:</b> Rule 5.1 only
     * <p>Allowed groups: MEMBERS_NEW or MODERATORS_PROFESSIONAL
     */
    @Override
    public Mono<AdminCreateUserResponse> createUserAsAdmin(
            AdminCreateUserRequest request,
            ViewerContext viewerContext) {
        return Mono.fromCallable(() -> {
                    // Normalize and validate inputs
                    String email = request.email().trim().toLowerCase();
                    String firstName = request.firstName().trim();
                    String lastName = request.lastName().trim();

                    // Generate username (auto-generate if not provided)
                    String username = request.username() != null ?
                            request.username().trim() :
                            generateUsername(firstName, lastName);

                    // Generate secure temporary password
                    String temporaryPassword = generateTemporaryPassword();

                    // Validate uniqueness (same as self-registration)
                    if (adminManager.findUserByEmail(email).isPresent()) {
                        throw new UserExistsException("An account already exists for this email.");
                    }

                    // Handle duplicate username by adding random suffix
                    if (adminManager.findUserByUsername(username).isPresent()) {
                        username = "%s.%d".formatted(username, ThreadLocalRandom.current().nextInt(100, 199));
                    }

                    // Security: RULE 5.1: New users only: MEMBERS_NEW or MODERATORS_PROFESSIONAL
                    validateNewInviteGroup(request.group(), viewerContext);
                    
                    // Create password credential
                    var passwordCred = adminManager.createPasswordCredential(temporaryPassword);

                    // Build user representation with pending actions
                    UserRepresentation userRep = new UserRepresentation();
                    userRep.setEnabled(true);
                    userRep.setUsername(username);
                    userRep.setEmail(email);
                    userRep.setFirstName(firstName);
                    userRep.setLastName(lastName);
                    userRep.setCredentials(List.of(passwordCred));
                    userRep.setEmailVerified(false); // Admin-created users need to verify
                    userRep.setRequiredActions(determineRequiredActions(request.group()));

                    // Create user in Keycloak
                    String userId = adminManager.createUser(userRep);

                    // Assign to specified group (not default /members/new)
                    adminManager.assignUserToGroup(userId, request.group());

                    // Package context for downstream processing
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
                // Create invitation in local database
                .flatMap(ctx -> Mono.fromCallable(() -> adminManager.findUserByUserId(ctx.userId)
                                .map(keycloakUserDtoMapper::mapToKeycloakUserDto)
                                .orElseThrow(() -> new UserDoesNotExistException("Failed to retrieve created user")))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(keycloakUserDto ->
                                adminInvitationService.createInvitation(keycloakUserDto, viewerContext.getUserId()))
                        .thenReturn(ctx))
                // Handle email invitation flow
                .flatMap(this::handleInvitationFlow);
    }

    /**
     * Reissues an admin invitation for a pending user.
     * Used when:
     * - The original invitation expired
     * - The user needs a new temporary password
     * - The admin wants to resend the invite with another email
     */
    @Override
    public Mono<AdminCreateUserResponse> reissueAdminInvitation(String userId, ReissueInvitationRequest request, ViewerContext viewerContext) {
        UUID userUUID = UUID.fromString(userId);
        String email = request.email().trim().toLowerCase();

        return Mono.fromCallable(() -> adminManager.findUserByUserId(userId)
                        .orElseThrow(UserDoesNotExistException::new))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userRep-> {
                    // Only check email uniqueness if it's actually changing
                    if(email.trim().equalsIgnoreCase(userRep.getEmail())){
                        return Mono.just(userRep);
                    }

                    return Mono.fromCallable(()-> adminManager.findUserByEmail(email))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(existingUser -> {
                                if(existingUser.isPresent()){
                                    return Mono.error(new UserDoesNotExistException("This email already belongs to another user."));
                                }
                                return Mono.just(userRep);
                            });
                })
                .zipWith(appUserRepository.existsByKeycloakId(userUUID))
                .flatMap(tuple -> {
                    UserRepresentation userRep = tuple.getT1();
                    boolean inAppUsers = tuple.getT2();

                    // Cannot reissue for already synced users
                    if (inAppUsers) {
                        throw new UserAlreadyActiveException();
                    }

                    // Cannot reissue for already verified users
                    if (Boolean.TRUE.equals(userRep.isEmailVerified())) {
                        throw new InvitationAlreadyVerifiedException();
                    }

                    // Security check: RULES 5.2, 5.3, 5.4, 7.1: Validate pending group transition
                    return  validatePendingInviteGroupTransition(userId, JsonNullable.of(request.group()),  viewerContext)
                            .thenReturn(userRep);
                })
                .flatMap(userRep -> Mono.fromCallable(() -> {
                    // Check if email is being changed
                    boolean isEmailChanged = setIfChangedStrict(
                            email,
                            userRep.getEmail(),
                            userRep::setEmail
                    );

                    // Update group if changed
                    if (request.group() != null) {
                        adminManager.assignUserToGroup(userId, request.group());
                    }

                    // Update Keycloak if email changed
                    if (isEmailChanged) {
                        adminManager.updateUser(userRep);
                    }

                    // Generate fresh temporary password
                    String newTempPassword = generateTemporaryPassword();
                    adminManager.resetPassword(userRep.getId(), newTempPassword);

                    // Fetch current group path
                    String groupPath = adminManager.getUserPrimaryGroupPath(userRep.getId());

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
                // Update invitation in local database
                .flatMap(ctx -> Mono.fromCallable(() -> adminManager.findUserByUserId(ctx.userId)
                                .map(keycloakUserDtoMapper::mapToKeycloakUserDto)
                                .orElseThrow(() -> new UserDoesNotExistException("Failed to retrieve user")))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(adminInvitationService::updateInvitation)
                        .thenReturn(ctx))
                // Reset invitation flags so the new temp password works
                .flatMap(ctx -> adminInvitationRepository.findByKeycloakId(UUID.fromString(ctx.userId))
                        .flatMap(adminInvitation -> {
                            adminInvitation.setIsInitialLogin(true);
                            adminInvitation.setCurrentStage(OnboardingStage.AWAITING_VERIFICATION);
                            return adminInvitationRepository.save(adminInvitation);
                        })
                        .thenReturn(ctx))
                // Send invitation email
                .flatMap(this::handleInvitationFlow);
    }

    /**
     * Updates a pending admin invitation.
     * Used for users still in the onboarding lobby (not yet synced to app_users).
     */
    @Override
    public Mono<PendingAdminInviteDto> updatePendingAdminInvite(String userId, UpdatePendingAdminInviteRequest request, ViewerContext viewerContext) {
        UUID userUUID = UUID.fromString(userId);

        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers -> {
                    // If user is already synced, use the synced user endpoint instead
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
                                // Users in AWAITING_VERIFICATION should use Reissue Invite instead
                                if (invitation.getCurrentStage() == OnboardingStage.AWAITING_VERIFICATION) {
                                    return Mono.error(new UserNotReadyException(
                                            "User hasn't verified email yet. Use 'Reissue Invite' to send a new invitation."
                                    ));
                                }

                                // Proceed with Keycloak update
                                return Mono.fromCallable(() -> adminManager.findUserByUserId(userId)
                                                .orElseThrow(UserDoesNotExistException::new))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(userRep ->
                                                validatePendingInviteGroupTransition(userId, request.getGroup(), viewerContext)
                                                        .flatMap(currentGroups -> applyUserUpdatesInKeycloak(userId, request.getGroup(), request.getIsEnabled(), userRep, currentGroups))
                                        );
                            })
                            .flatMap(adminInvitationService::updateInvitation);
                });
    }

    /**
     * Updates a user as an admin.
     * Used for fully onboarded users who are already synced to app_users.
     */
    @Override
    public Mono<KeycloakUserDto> updateUserAsAdmin(String userId, AdminUpdateUserRequest request, ViewerContext viewerContext) {
        UUID userUUID = UUID.fromString(userId);

        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers -> {
                    // If not in app_users, check if they're in the lobby
                    if (!inAppUsers) {
                        return adminInvitationRepository.existsByKeycloakId(userUUID)
                                .flatMap(inLobby -> {
                                    if (inLobby) {
                                        return Mono.error(new UserNotReadyException(
                                                "Cannot modify profile: User has not completed onboarding. " +
                                                        "Use 'Reissue Invite' to manage lobby users."
                                        ));
                                    } else {
                                        return Mono.error(new UserDoesNotExistException(
                                                "User not found in the local system. Please ensure they're logged in at least once"
                                        ));
                                    }
                                });
                    }

                    // Proceed with Keycloak update
                    return Mono.fromCallable(() -> adminManager.findUserByUserId(userId)
                                    .orElseThrow(UserDoesNotExistException::new))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(userRep ->
                                    validateSyncedUserModification(userId, request.getGroup(), request.getIsEnabled(), viewerContext)
                                            .flatMap(currentGroups -> applyUserUpdatesInKeycloak(userId, request.getGroup(), request.getIsEnabled(), userRep, currentGroups))
                            );
                });
    }

    /**
     * Revokes an invitation for a pending user.
     * Permanently removes the user from Keycloak and marks invitation as complete.
     */
    @Override
    public Mono<Void> revokeInvitation(String userId) {
        UUID userUUID = UUID.fromString(userId);
        return appUserRepository.existsByKeycloakId(userUUID)
                .flatMap(inAppUsers ->
                        Mono.fromCallable(() -> {
                                    // Fetch latest state from Keycloak
                                    UserRepresentation userRep = adminManager.findUserByUserId(userId)
                                            .orElseThrow(UserDoesNotExistException::new);

                                    // Cannot revoke if already synced
                                    if (inAppUsers) {
                                        throw new UserAlreadyActiveException();
                                    }

                                    // Cannot revoke if already verified
                                    if (Boolean.TRUE.equals(userRep.isEmailVerified())) {
                                        throw new InvitationAlreadyVerifiedException();
                                    }

                                    // Delete from Keycloak
                                    adminManager.deleteUser(userId);
                                    return userId;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(id -> adminInvitationService.completeInvitation(UUID.fromString(id)))
                );
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Handles the invitation flow for a newly created or reissued user.
     * Creates a verification link and optionally triggers a Novu email.
     */
    private Mono<AdminCreateUserResponse> handleInvitationFlow(AdminUserContext ctx) {
        return verificationService.createVerificationLink(ctx.email, VerificationType.INVITED, ctx.groupPath, null)
                .flatMap(invitationLink -> {
                    // Only send email if the admin requested it
                    if (ctx.sendInvitationEmail) {
                        AdminInvitePayload payload = new AdminInvitePayload(
                                ctx.firstName,
                                ctx.tempPassword,
                                invitationLink,
                                ctx.groupPath
                        );

                        return novuService.triggerEvent(NovuWorkflow.ADMIN_ONBOARDING_INVITE, ctx.userId, ctx.email, payload)
                                .map(sentStatus -> new AdminCreateUserResponse(
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

    /**
     * Generates a username from first and last names.
     * Normalizes Unicode characters and removes invalid characters.
     * Falls back to manual username requirement if generation fails.
     */
    private String generateUsername(String firstName, String lastName) {
        // Normalize Unicode characters (é → e, ç → c)
        String normalizedFirstName = normalizeUnicode(firstName.toLowerCase());
        String normalizedLastName = normalizeUnicode(lastName.toLowerCase());

        String base = "%s.%s".formatted(normalizedFirstName, normalizedLastName);

        // Remove invalid characters (only allow letters, numbers, dots, underscores)
        String cleaned = base.replaceAll("[^a-z0-9._]", "");
        // Remove leading/trailing dots and underscores
        cleaned = cleaned.replaceAll("^[._]+|[._]+$", "");
        // Replace multiple consecutive dots/underscores with single
        cleaned = cleaned.replaceAll("[._]{2,}", ".");

        // Validate generated username
        if (cleaned.isEmpty()) {
            throw new UsernameGenerationException(
                    String.format(
                            "Could not generate a valid username from names: '%s %s'. " +
                                    "Please provide a username manually.",
                            firstName, lastName
                    )
            );
        } else if (cleaned.length() < 3) {
            throw new UsernameGenerationException(
                    String.format(
                            "Could not generate username '%s' is too short (minimum 3 characters). " +
                                    "Please provide a username manually.",
                            cleaned
                    )
            );
        }

        // Truncate to maximum length
        return cleaned.substring(0, Math.min(cleaned.length(), 30));
    }

    /**
     * Generates a secure temporary password with at least one of each character type.
     * Uses character sets that avoid ambiguous characters (no 0, O, I, l, 1, etc.).
     */
    private String generateTemporaryPassword() {
        final int PASSWORD_LENGTH = 12;

        // Character sets that avoid ambiguous characters
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

        // Fill the rest with random characters
        String allChars = upper + lower + digits + special;
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle to randomize positions
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * Determines required actions for a user based on their group.
     * Currently, all admin-created users must verify their email.
     */
    private List<String> determineRequiredActions(GroupPath group) {
        List<RequiredAction> actions = new ArrayList<>();
        actions.add(RequiredAction.VERIFY_EMAIL);
        return actions.stream().map(Enum::name).toList();
    }

    // ============================================================
    // PENDING USER OPERATIONS
    // ============================================================

    /**
     * Validates that a group is allowed for pending users
     *
     * <p><b>Rules Applied:</b> Rules 5.3, 5.4
     * <p>Allowed: MEMBERS_NEW, MODERATORS_PROFESSIONAL
     * <p>Blocked: ADMINISTRATORS, SUPER_ADMINISTRATORS (Rule 5.3)
     * */
    private void validateNewInviteGroup(GroupPath targetGroup, ViewerContext viewerContext) {
        // Rule 5.3: Block administrative users
        if(targetGroup == GroupPath.ADMINISTRATORS || targetGroup == GroupPath.SUPER_ADMINISTRATORS){
            throw new InsufficientPermissionException(
                    String.format(
                            "Pending users cannot be assigned administrative tier '%s'. Only '%s' or '%s' are allowed",
                                targetGroup.getDisplayName(),
                                GroupPath.MEMBERS_NEW.getDisplayName(),
                                GroupPath.MODERATORS_PROFESSIONAL.getDisplayName()
                            )
            );
        }

        // Rule 5.1/5.2/5.4: Only MEMBERS_NEW or MODERATORS_PROFESSIONAL allowed for pending
        if(targetGroup != GroupPath.MEMBERS_NEW && targetGroup != GroupPath.MODERATORS_PROFESSIONAL){
            throw new InsufficientPermissionException(
                    String.format("Pending users can only be assigned to '%s' or '%s'. Received: '%s'",
                            GroupPath.MEMBERS_NEW.getDisplayName(),
                            GroupPath.MODERATORS_PROFESSIONAL.getDisplayName(),
                            targetGroup.getDisplayName())
            );
        }

        // Check superAdmin permission for MODERATOR_PROFESSIONAL
        if(targetGroup == GroupPath.MODERATORS_PROFESSIONAL && !viewerContext.isSuperAdmin()){
            throw new InsufficientPermissionException(
                    String.format("Only superadmins can assign '%s' group.",
                            GroupPath.MODERATORS_PROFESSIONAL.getDisplayName())
            );
        }

    }

    /**
     * Validates a pending invite group transition
     *
     * <p><b>Rules Applied:</b> Rules 5.2, 5.3, 5.4, 7.1
     * <p>Free movement between MEMBERS_NEW and MODERATORS_PROFESSIONAL.
     * <p>Correction: Can correct from admin tiers back to allowed groups.
     * <p>Blocked: Cannot assign admin tiers.
     * */
    private Mono<List<String>> validatePendingInviteGroupTransition(
            String targetUserId,
            JsonNullable<GroupPath> targetGroup,
            ViewerContext viewerContext
    ) {
        return Mono.fromCallable(() -> {
                    // Get target user's current groups
                    List<String> currentGroups = adminManager.getUserGroups(targetUserId);
                    GroupPath currentGroup = getPrimaryGroup(currentGroups);

                    if(targetGroup != null && targetGroup.isPresent()){
                        GroupPath newGroup = targetGroup.get();

                        // Rule 5.3: Block administrative users
                        if(newGroup == GroupPath.ADMINISTRATORS || newGroup == GroupPath.SUPER_ADMINISTRATORS){
                            throw new InsufficientPermissionException(
                                    String.format(
                                            "Reissue invite cannot assign administrative tier '%s'. Only '%s' or '%s' are allowed",
                                            newGroup.getDisplayName(),
                                            GroupPath.MEMBERS_NEW.getDisplayName(),
                                            GroupPath.MODERATORS_PROFESSIONAL.getDisplayName()
                                    )
                            );
                        }

                        // Rules 5.2, 5.4: Allow free movement between MEMBERS_NEW and MODERATORS_PROFESSIONAL
                        if(newGroup != GroupPath.MEMBERS_NEW && newGroup != GroupPath.MODERATORS_PROFESSIONAL){
                            throw new InsufficientPermissionException(
                                    String.format("Reissue can only be assigned to '%s' or '%s'. Received: '%s'",
                                            GroupPath.MEMBERS_NEW.getDisplayName(),
                                            GroupPath.MODERATORS_PROFESSIONAL.getDisplayName(),
                                            newGroup.getDisplayName())
                            );
                        }

                        // Check superAdmin permission for MODERATOR_PROFESSIONAL
                        if(newGroup == GroupPath.MODERATORS_PROFESSIONAL && !viewerContext.isSuperAdmin()){
                            throw new InsufficientPermissionException(
                                    String.format("Only superadmins can assign '%s' group.",
                                            GroupPath.MODERATORS_PROFESSIONAL.getDisplayName())
                            );
                        }

                        // Correction scenario: If current group is an admin tier, allow correction
                        if(currentGroup == GroupPath.ADMINISTRATORS || currentGroup == GroupPath.SUPER_ADMINISTRATORS){
                            log.warn("Correcting improperly assigned administrative tier: '{}' to pending group '{}' for user",
                                    currentGroup.getDisplayName(), newGroup.getDisplayName());

                            // Allow the correction
                        }

                    }

                    return currentGroups;
                }).subscribeOn(Schedulers.boundedElastic());
    }

    // ============================================================
    // SYNCED USER OPERATIONS
    // ============================================================


    /**
     *  Master validation for synced user modifications.
     * Enforces all security rules in the correct order:
     *   1. Self-modification checks
     *   2. Same-level protection
     *   3. Superadmin protection
     *   4. Hierarchy progression
     *   5. System integrity (last-user safeguard)
     *
     *   <p><b>Rules Applied:</b> 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2
     */
    private Mono<List<String>> validateSyncedUserModification(
            String targetUserId,
            JsonNullable<GroupPath> targetGroup,
            JsonNullable<Boolean> isEnabled,
            ViewerContext viewerContext
    ) {
        return Mono.fromCallable(() -> {
                    // Get target user's current groups
                    List<String> targetCurrentGroups = adminManager.getUserGroups(targetUserId);
                    GroupPath targetCurrentGroup = getPrimaryGroup(targetCurrentGroups);
                    GroupPath viewerGroup = getPrimaryGroup(viewerContext.getGroups());

                    // 1. Self-modification Protection (Rules 2.1, 2.2)
                    validateSelfModification(targetUserId, targetCurrentGroup, targetGroup, isEnabled, viewerContext);

                    // 2. Same-level Protection (Rules 3.1, 3.2) — SKIP for self-modification
                    validateSameLevelProtection(targetUserId, targetCurrentGroup, viewerGroup, viewerContext);

                    // 3. Superadmin Protection
                    validateSuperAdminProtection(targetCurrentGroups,  viewerContext);

                    // 4. Hierarchy Progression (Rules 1.1, 1.2, 1.3)
                    validateHierarchyProgression(targetGroup, targetCurrentGroup);

                    // 5. Only superadmins can assign admin-level groups
                    validateGroupAssignmentPermission(targetGroup, viewerContext);

                    return targetCurrentGroups;
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(currentGroups ->
                        // 5. Last-User Safeguard (Rules 4.1, 4.2)
                        validateLastUserSafeguard(targetGroup, isEnabled, currentGroups)
                );
    }

    // ============================================================
    // VALIDATION METHODS (Shared)
    // ============================================================

    // Gets the user's primary group from a list of groups, defaulting to MEMBERS_NEW
    private GroupPath getPrimaryGroup(List<String> groups) {
        return groups.stream()
                .map(GroupPath::fromPath)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(GroupPath.MEMBERS_NEW);
    }

    // Gets the user's primary group from a set of groups, defaulting to MEMBERS_NEW
    private GroupPath getPrimaryGroup(Set<String> groups) {
        return groups.stream()
                .map(GroupPath::fromPath)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(GroupPath.MEMBERS_NEW);
    }

    // Users cannot demote, promote, or disable their own accounts.
    // Superadmins and regular admins are completely blocked from self-modification.
    // Self-modification validation (Rules 2.1, 2.2).
    private void validateSelfModification(
            String targetUserId,
            GroupPath targetCurrentGroup,
            JsonNullable<GroupPath> targetGroup,
            JsonNullable<Boolean> isEnabled,
            ViewerContext viewerContext
    ) {
        if (!isSelfModification(targetUserId, viewerContext)) {
            return;
        }

        // Cannot change group at all (promotion or demotion)
        if (targetGroup != null && targetGroup.isPresent()) {
            GroupPath newGroup = targetGroup.get();

            if (GroupPath.isPromotion(targetCurrentGroup, newGroup)) {
                throw new InsufficientPermissionException(
                        String.format("You cannot promote yourself from '%s' to '%s'. Promotions require superadmin approval.",
                                targetCurrentGroup.getDisplayName(),
                                newGroup.getDisplayName())
                );
            }

            // Self-demotion is allowed! The last-user safeguard will prevent removing the last super-admin
            if (GroupPath.isDemotion(targetCurrentGroup, newGroup)) {
                log.info("User {} is demoting themselves from {} to {}",
                        viewerContext.getUserId(),
                        targetCurrentGroup.getDisplayName(),
                        newGroup.getDisplayName());
            }
        }

        // Cannot disable own account
        if (isEnabled != null && isEnabled.isPresent() && Boolean.FALSE.equals(isEnabled.get())) {
            throw new InsufficientPermissionException(
                    "You cannot disable your own account."
            );
        }
    }

    // Superadmins cannot modify other superadmins. Admins cannot modify other admins.
    private void validateSameLevelProtection(
            String targetUserId,
            GroupPath targetCurrentGroup,
            GroupPath viewerGroup,
            ViewerContext viewerContext) {

        // If it's self-modification, skip same-level protection
        // This allows admins and superadmins to demote themselve
        if (isSelfModification(targetUserId, viewerContext)) {
            return;
        }

        // Superadmin peer protection
        if (targetCurrentGroup == GroupPath.SUPER_ADMINISTRATORS && viewerGroup == GroupPath.SUPER_ADMINISTRATORS) {
            throw new InsufficientPermissionException(
                    "Superadmins cannot modify other superadmin users."
            );
        }

        // Admin peer protection
        if (targetCurrentGroup == GroupPath.ADMINISTRATORS && viewerGroup == GroupPath.ADMINISTRATORS) {
            throw new InsufficientPermissionException(
                    "Admins cannot modify other admin users."
            );
        }
    }

    // Only superadmins can modify users in admin-level groups.
    private void validateSuperAdminProtection(
            List<String> currentGroups,
            ViewerContext viewerContext
    ) {
        boolean isTargetSuperAdmin = currentGroups.stream().anyMatch(
                groupPath -> GroupPath.SUPER_ADMINISTRATORS.getPath().equals(groupPath)
        );

        boolean isTargetAdmin = currentGroups.stream().anyMatch(
                groupPath -> GroupPath.ADMINISTRATORS.getPath().equals(groupPath) ||
                        GroupPath.SUPER_ADMINISTRATORS.getPath().equals(groupPath)
        );

        // Only superadmin can modify superadmin
        if (isTargetSuperAdmin && !viewerContext.isSuperAdmin()) {
            throw new InsufficientPermissionException(
                    "Only superadmin can modify a superadmin user"
            );
        }

        // Only superadmin can modify admin
        if (isTargetAdmin && !viewerContext.isAdmin()) {
            throw new InsufficientPermissionException(
                    "Only superadmin can modify an admin user"
            );
        }
    }

    // Users can only move one level up or down at a time.
    // Hierarchy progression (Rules 1.1, 1.2, 1.3)
    private void validateHierarchyProgression(
            JsonNullable<GroupPath> targetGroup,
            GroupPath targetCurrentGroup
    ) {
        if (targetGroup == null || !targetGroup.isPresent()) {
            return;
        }

        GroupPath newGroup = targetGroup.get();

        // Allow no-change updates
        if (targetCurrentGroup == newGroup) {
            return;
        }

        // Enforce single-step transitions
        if (!GroupPath.isValidTransition(targetCurrentGroup, newGroup)) {
            throw new InsufficientPermissionException(
                    String.format(
                            "Cannot move from '%s' to '%s'. Users can only progress one level at a time.",
                            targetCurrentGroup.getDisplayName(),
                            newGroup.getDisplayName()
                    )
            );
        }

    }

    // Only superadmins can assign ADMINISTRATORS or SUPER_ADMINISTRATORS.
    private void validateGroupAssignmentPermission(JsonNullable<GroupPath> targetGroup, ViewerContext viewerContext) {

        if (targetGroup != null && targetGroup.isPresent()) {
            GroupPath newGroup = targetGroup.get();

            if (!GroupPath.isSuperAdminOnlyGroup(newGroup, viewerContext)) {
                throw new InsufficientPermissionException(
                        String.format("Only superadmins can assign the '%s' group.",
                                newGroup.getDisplayName())
                );
            }

        }

    }

    // Prevents removal of the last active superadmin.
    // This is a defensive safety net - under normal operation, superadmin modifications
    // are blocked by other validation methods upstream.
    // Last-user safeguard (Rules 4.1, 4.2)
    private Mono<List<String>> validateLastUserSafeguard(
            JsonNullable<GroupPath> targetGroup,
            JsonNullable<Boolean> isEnabled,
            List<String> currentGroups
    ) {
        boolean isTargetSuperAdmin = currentGroups.stream().anyMatch(
                groupPath -> GroupPath.SUPER_ADMINISTRATORS.getPath().equals(groupPath)
        );

        if (!isTargetSuperAdmin) {
            return Mono.just(currentGroups);
        }

        // Check if this is a destructive action
        boolean isDestructiveAction = false;

        if (targetGroup != null && targetGroup.isPresent()) {
            GroupPath newGroup = targetGroup.get();
            if (newGroup != GroupPath.SUPER_ADMINISTRATORS) {
                isDestructiveAction = true;
            }
        }

        if (isEnabled != null && isEnabled.isPresent() && Boolean.FALSE.equals(isEnabled.get())) {
            isDestructiveAction = true;
        }

        if (!isDestructiveAction) {
            return Mono.just(currentGroups);
        }

        // Safety net: prevent removing the last superadmin
        return countActiveSuperAdmins()
                .flatMap(activeCount -> {
                    if (activeCount <= 1) {
                        return Mono.error(new SystemIntegrityException(
                                String.format(
                                        "Cannot remove the last active superadmin. " +
                                                "At least one superadmin must remain active. (Active superadmins: %d)",
                                        activeCount
                                )
                        ));
                    }
                    return Mono.just(currentGroups);
                });
    }

    // Counts active superadmins (in SUPER_ADMINISTRATORS group and enabled)
    private Mono<Integer> countActiveSuperAdmins() {
        return Mono.fromCallable(() -> {
            List<UserRepresentation> superAdmins = adminManager.getUsersInGroups(
                    GroupPath.SUPER_ADMINISTRATORS
            );
            return (int) superAdmins.stream()
                    .filter(UserRepresentation::isEnabled)
                    .count();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // Checks if the viewer is modifying themselves
    private boolean isSelfModification(String targetUserId, ViewerContext viewerContext) {
        return targetUserId.equals(viewerContext.getUserId());
    }

    // ============================================================
    // SHARED UPDATE LOGIC
    // ============================================================

    /**
     * Applies user updates to Keycloak.
     * Handles both group and enabled status changes.
     * This is the shared update logic used by both pending and synced user flows.
     */
    private Mono<KeycloakUserDto> applyUserUpdatesInKeycloak(
            String userId,
            JsonNullable<GroupPath> group,
            JsonNullable<Boolean> isEnabled,
            UserRepresentation userRep,
            List<String> currentGroups
    ){
        return Mono.fromCallable(() -> {
            // Handle enabled status change
            boolean isEnabledChanged = patchStrict(
                    isEnabled,
                    userRep.isEnabled(),
                    userRep::setEnabled
            );

            // Handle group change
            boolean isGroupChanged = false;
            if (group != null && group.isPresent()) {
                GroupPath targetGroupPath = group.get();

                String targetGroupPathStr = targetGroupPath.getPath();
                isGroupChanged = currentGroups.isEmpty()
                        || !currentGroups.contains(targetGroupPathStr);

                if (isGroupChanged) {
                    adminManager.assignUserToGroup(userId, targetGroupPath);
                    log.info("Updated group for user {} to {}", userId, targetGroupPathStr);
                }
            }

            // Update Keycloak if anything changed
            if (isEnabledChanged || isGroupChanged) {
                adminManager.updateUser(userRep);
                log.info("Successfully updated profile for user ID: {}", userId);
            } else {
                log.debug("No profile changes detected for user ID: {}", userId);
            }

            return keycloakUserDtoMapper.mapToKeycloakUserDto(userRep);
        }).subscribeOn(Schedulers.boundedElastic());
    }


}