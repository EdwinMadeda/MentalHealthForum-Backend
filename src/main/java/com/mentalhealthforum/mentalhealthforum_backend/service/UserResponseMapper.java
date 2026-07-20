




package com.mentalhealthforum.mentalhealthforum_backend.service;

import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.timezone.TimezoneDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ProfileVisibility;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    private static final boolean ENFORCE_ADMIN_TRANSPARENCY = true;
    private final TimezoneService timezoneService;

    public UserResponseMapper(TimezoneService timezoneService) {
        this.timezoneService = timezoneService;
    }

    // ---------------------  public entry points ---------------------------
    public UserResponse mapUserBasedOnContext(AppUserEntity targetUser, ViewerContext viewerContext) {
        if(viewerContext == null){
            return  toPublicUserResponse(targetUser);
        }

        boolean isSelf = Boolean.TRUE.equals(targetUser.getIsSelf()) ||
                (viewerContext.getUserId() != null &&
                        viewerContext.getUserId().equals(targetUser.getKeycloakId().toString()));


        return isSelf
                ? toSelfResponse(targetUser)
                : toOtherResponse(targetUser, viewerContext);
    }

    public UserResponse toSelfResponse(AppUserEntity targetUser){
        UserResponse response = buildBaseResponse(targetUser, true);
        setPrivateFields(response, targetUser);
        response.setProfileVisibility(targetUser.getProfileVisibility());
        return response;
    }

    // --------------------- private entry points ---------------------------

    private UserResponse toPublicUserResponse(AppUserEntity targetUser){
        return buildBaseResponse(targetUser, false);
    }

    private UserResponse toOtherResponse(AppUserEntity targetUser, ViewerContext viewerContext){
        UserResponse response = buildBaseResponse(targetUser, false);
        response.setUsername(null);
        response.setEmail(null);
        response.setPendingEmail(null);

        // Admin transparency: viewer or target is admin/moderator show all extended fields
        if(ENFORCE_ADMIN_TRANSPARENCY && isAdminOrModerator(targetUser, viewerContext)){
            setExtendedFields(response, targetUser);
            response.setProfileVisibility(targetUser.getProfileVisibility());
            return response;
        }

        // Respect the entity's effective visibility (already forced for admins/moderators)
        return applyPrivacyForOther(response, targetUser);
    }

    // ---------- private builders ----------

    private UserResponse buildBaseResponse(AppUserEntity appUser, boolean isSelf) {
        UserResponse response = new UserResponse(
                appUser.getKeycloakId(),
                appUser.getDateJoined(),
                isSelf,
                appUser.getDisplayName(),
                appUser.getPostsCount(),
                appUser.getReputationScore(),
                appUser.getLastActiveAt(),
                appUser.getLastPostedAt(),
                appUser.getIsActive(),
                appUser.getRoles(),
                appUser.getGroups()
        );
        response.setInitials(appUser.getInitials());
        return response;
    }

    private void setPrivateFields(UserResponse response, AppUserEntity appUser) {
        response.setUsername(appUser.getUsername());
        response.setEmail(appUser.getEmail());
        response.setFirstName(appUser.getFirstName());
        response.setLastName(appUser.getLastName());
        response.setAvatarUrl(appUser.getAvatarUrl());
        response.setBio(appUser.getBio());
        response.setTimezone(appUser.getTimezone());
        setTimezoneDetails(response, appUser);
        response.setLanguage(appUser.getLanguage());
        response.setPendingEmail(appUser.getPendingEmail());
        response.setLastLoginAt(appUser.getLastLoginAt());
    }

    private void setExtendedFields(UserResponse response, AppUserEntity appUser) {
        response.setFirstName(appUser.getFirstName());
        response.setLastName(appUser.getLastName());
        response.setAvatarUrl(appUser.getAvatarUrl());
        response.setBio(appUser.getBio());
        response.setTimezone(appUser.getTimezone());
        setTimezoneDetails(response, appUser);
        response.setLanguage(appUser.getLanguage());
    }

    private UserResponse applyPrivacyForOther(UserResponse response, AppUserEntity targetUser) {

        ProfileVisibility effectiveVisibility = targetUser.getProfileVisibility();

        /*
                           Visibility already enforced in entity for admin/moderator
           MEMBERS_ONLY:   Visible to all logged‑in members → show all extended fields
           CONNECTED_ONLY: Only visible to connected members → show all extended fields if connected
           PRIVATE:        Strict privacy – only self or admins/mods (handled earlier in toOtherResponse).  For "others", we hide extended fields even if connected

           (Future: other visibility values can be added)

        * */

        boolean showExtendedFields = switch (effectiveVisibility){
            case MEMBERS_ONLY -> true;
            case CONNECTED_ONLY -> Boolean.TRUE.equals(targetUser.getIsConnected());
            case PRIVATE -> false;
        };

        if(showExtendedFields){
            setExtendedFields(response, targetUser);
            response.setProfileVisibility(effectiveVisibility);
        }
        else{
            // PRIVATE (or unexpected) - hide everything
            response.setFirstName(null);
            response.setLastName(null);
            response.setAvatarUrl(null);
            response.setBio(null);
            response.setTimezone(null);
            response.setTimezoneDetails(null);
            response.setLanguage(null);
            response.setProfileVisibility(ProfileVisibility.PRIVATE);
        }

        return response;
    }

    // ---------- helpers ----------

    private void setTimezoneDetails(UserResponse response, AppUserEntity appUser) {
        if (appUser.getTimezone() != null) {
            TimezoneDetails details = timezoneService.getTimezoneDetails(appUser.getTimezone());
            response.setTimezoneDetails(details);
        } else {
            response.setTimezoneDetails(null);
        }
    }

    private boolean isAdminOrModerator(AppUserEntity appUser, ViewerContext viewer) {
        return appUser.isModeratorOrAdmin() || viewer.isModeratorOrAdmin();
    }
}
