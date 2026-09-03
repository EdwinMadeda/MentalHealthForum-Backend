package com.mentalhealthforum.mentalhealthforum_backend.enums;

import com.mentalhealthforum.mentalhealthforum_backend.service.PrivilegedUser;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * GroupPath defines all user groups in the system along with their role mappings.

 * ============================================================
 * GROUP ASSIGNMENT RULES (Security Model)
 * ============================================================

 * 1. Superadmin-Only Groups (can only be assigned by SUPER_ADMIN):
 *    - ADMINISTRATORS
 *    - SUPER_ADMINISTRATORS

 * 2. Regular Admin Groups (can be assigned by any admin):
 *    - MEMBERS_NEW, MEMBERS_ACTIVE, MEMBERS_TRUSTED
 *    - MODERATORS_PEER, MODERATORS_PROFESSIONAL

 * 3. Superadmin Protection:
 *    - Regular admins CANNOT modify any user who is in SUPER_ADMINISTRATORS
 *    - Only superadmins can modify superadmin users

 * 4. Visibility:
 *    - Regular admins CANNOT see SUPER_ADMINISTRATORS in dropdowns
 *    - Superadmins CAN see all groups in dropdowns

 * These rules are enforced in:
 * - GroupPath.isSuperAdminOnlyGroup()      → Core rule logic
 * - GroupPath.getAssignableGroupsForViewer() → Dropdown visibility
 * - AdminUserServiceImpl.updateUserInKeycloak() → Assignment enforcement
 * - AdminUserServiceImpl.createUserAsAdmin() → Creation enforcement
 * - AdminUserServiceImpl.reissueAdminInvitation() → Reissue enforcement
 * ============================================================
 */
@Getter
public enum GroupPath {
    // Root Groups
    MEMBERS("/members", "General members"),
    MODERATORS("/moderators", "Content moderators"),
    ADMINISTRATORS("/administrators", "Administrators"),
    SUPER_ADMINISTRATORS("/super_administrators", "Super Administrators"),

    // Subgroups - Members
    MEMBERS_NEW("/members/new", "New members"),
    MEMBERS_ACTIVE("/members/active", "Active members"),
    MEMBERS_TRUSTED("/members/trusted", "Trusted members"),

    // Subgroups - Moderators
    MODERATORS_PEER("/moderators/peer", "Peer moderators"),
    MODERATORS_PROFESSIONAL("/moderators/professional", "Professional moderators");

    private final String path;
    private final String description;

    GroupPath(String path, String description){
        this.path = path;
        this.description = description;
    }

    public String getDisplayName(){
        return this.description;
    }

    public static String getFriendlyName(String groupPath){
        GroupPath group = GroupPath.fromPath(groupPath);
        return (group != null) ? group.getDisplayName(): "our community";
    }

    public static GroupPath fromPath(String path){
        for(GroupPath group: values()){
            if(group.path.equals(path)){
                return group;
            }
        }
        return null;
    }

    public static boolean isInGroup(String userGroupPath, GroupPath targetGroup){
        if(userGroupPath == null || targetGroup == null) return false;

        // For administrators, exact match
        if(targetGroup == ADMINISTRATORS){
            return userGroupPath.equals(targetGroup.getPath());
        }

        // For other groups, check if user's group starts with the target group path
        return userGroupPath.startsWith(targetGroup.getPath());
    }

    public static List<GroupPath> getSubgroups(GroupPath parentGroup){
        return Arrays.stream(values())
                .filter(group ->group.path.startsWith(parentGroup.getPath() + "/"))
                .toList();
    }

    // Helper method to check if a path contains a group
    public static boolean pathContainsGroup(String userGroupPath, GroupPath targetGroup){
        if(userGroupPath == null || targetGroup == null) return false;
        return userGroupPath.contains(targetGroup.getPath());
    }


    // --- CORE: Role Mapping (Based on Keycloak Configuration) ---

    /**
     * Returns the list of RealmRoles automatically granted to users in this group.
     * Based on the Keycloak realm configuration where groups define role inheritance.
     * Which groups actually grant roles?
     * Important: Groups grant ALL listed roles cumulatively, not a choice.
     * Example: MEMBERS_TRUSTED grants FORUM_MEMBER, TRUSTED_MEMBER, AND PEER_SUPPORTER.
     *
     * @return List of RealmRoles granted by this group, empty list for parent/non-assignable groups
     */
    public List<RealmRole> getGrantedRoles(){
        return switch(this){
            // Member subgroups
            case MEMBERS_NEW -> List.of(RealmRole.FORUM_MEMBER);
            case MEMBERS_ACTIVE -> List.of(RealmRole.FORUM_MEMBER, RealmRole.TRUSTED_MEMBER);
            case MEMBERS_TRUSTED -> List.of(RealmRole.FORUM_MEMBER, RealmRole.TRUSTED_MEMBER, RealmRole.PEER_SUPPORTER);

            // Moderator subgroups
            case MODERATORS_PEER -> List.of(RealmRole.FORUM_MEMBER, RealmRole.MODERATOR);
            case MODERATORS_PROFESSIONAL -> List.of(RealmRole.FORUM_MEMBER, RealmRole.PEER_SUPPORTER, RealmRole.MODERATOR);

            // Administrator group
            case ADMINISTRATORS -> List.of(RealmRole.ADMIN, RealmRole.SUPER_ADMIN);

            // Super-administrator group
            case SUPER_ADMINISTRATORS -> List.of(RealmRole.ADMIN, RealmRole.SUPER_ADMIN);

            // Parent groups grant NO roles - this is intentional
            case MEMBERS, MODERATORS -> List.of();
        };
        }

    /**
     * Determines if this group can be directly assigned to users.
     * Only leaf groups with actual role grants should be assignable.
     * Parent groups (MEMBERS, MODERATORS) are NOT assignable.
     * Can users be assigned to this group?
     * @return true if group grants roles and can be assigned to users
     */
    public boolean isAssignable(){
        return !getGrantedRoles().isEmpty();
    }

    /**
     * Checks if this group grants a specific RealmRole.
     *
     * @param role The RealmRole to check
     * @return true if the role is in this group's granted roles list
     */
    public boolean grantsRoles(RealmRole role){
        return getGrantedRoles().contains(role);
    }

    // --- STATIC UTILITY METHODS (For Validation and UI) ---

    /**
     * Returns all groups that can be assigned to users.
     * Used for admin UI dropdowns and API validation.
     *
     * @return List of assignable GroupPaths (leaf groups with role grants)
     */
    public static List<GroupPath> getAssignableGroups(){
        return Arrays.stream(values())
                .filter(GroupPath::isAssignable)
                .toList();
    }

    public static boolean isSuperAdminOnlyGroup(GroupPath groupPath, PrivilegedUser viewer){
        Set<GroupPath> superAdminOnlyGroups = Set.of(
                ADMINISTRATORS,
                SUPER_ADMINISTRATORS
        );

        if(superAdminOnlyGroups.contains(groupPath)){
            return viewer != null && viewer.isSuperAdmin();
        }
        return true;
    }

    public static List<GroupPath> getAssignableGroupsForViewer(PrivilegedUser viewer){
        return getAssignableGroups().stream()
                .filter(group -> isSuperAdminOnlyGroup(group, viewer))
                .toList();
    }


    /**
     * Validates if a group path string represents an assignable group.
     *
     * @param path The group path string to validate
     * @return true if the path corresponds to an assignable group
     */
    public static boolean isValidAssignableGroup(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        GroupPath group = fromPath(path);
        return group != null && group.isAssignable();
    }
    
}




