package com.mentalhealthforum.mentalhealthforum_backend.enums;

import com.mentalhealthforum.mentalhealthforum_backend.service.PrivilegedUser;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * GroupPath defines all user groups in the system along with their role mappings.

 * ============================================================
 * HIERARCHY CHAIN
 * ============================================================
 * MEMBERS_NEW → MEMBERS_ACTIVE → MEMBERS_TRUSTED →
 * MODERATORS_PEER → MODERATORS_PROFESSIONAL →
 * ADMINISTRATORS → SUPER_ADMINISTRATORS

 * ============================================================
 * TRANSITION RULES (Enforced by isValidTransition)
 * ============================================================
 * - Single-step: Can only move one level up or down at a time
 * - Professional Exception: Direct assignment to MODERATORS_PROFESSIONAL allowed from any lower tier
 * - Admin Tiers: ADMINISTRATORS and SUPER_ADMINISTRATORS are handled by superadmin checks

 * ============================================================
 * VISIBILITY RULES
 * ============================================================
 * - Regular admins CANNOT see ADMINISTRATORS or SUPER_ADMINISTRATORS in dropdowns
 * - Superadmins CAN see all groups in dropdowns

 * ============================================================
 * ASSIGNABLE GROUPS
 * ============================================================
 * - Only leaf groups with role grants are assignable (isAssignable())
 * - Parent groups (MEMBERS, MODERATORS) grant NO roles and are NOT assignable

 * ============================================================
 * ROLE MAPPINGS
 * ============================================================
 * - Each assignable group grants specific RealmRoles via getGrantedRoles()
 * - Roles are cumulative (e.g., MEMBERS_TRUSTED grants FORUM_MEMBER, TRUSTED_MEMBER, AND PEER_SUPPORTER)
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

    /**
     * Returns the natural progression order of groups.
     * Users should progress through these levels in order.
     */
    public static List<GroupPath> getHierarchy() {
        return List.of(
                MEMBERS_NEW,
                MEMBERS_ACTIVE,
                MEMBERS_TRUSTED,
                MODERATORS_PEER,
                MODERATORS_PROFESSIONAL,
                ADMINISTRATORS,
                SUPER_ADMINISTRATORS
        );
    }

    /**
     * Gets the index of a group in the hierarchy.
     * Returns -1 if the group is not in the hierarchy.
     */
    public static int getHierarchyLevel(GroupPath group){
        List<GroupPath> hierarchy = getHierarchy();
        return hierarchy.indexOf(group);
    }

    /**
     * Checks if moving from currentGroup to newGroup would be a promotion.
     * Same level is NOT considered a promotion.
     */
    public static boolean isPromotion(GroupPath currentGroup, GroupPath newGroup){
        if(currentGroup == null || newGroup == null){
            return false;
        }
        return getHierarchyLevel(newGroup) > getHierarchyLevel(currentGroup);
    }

    /**
     * Checks if moving from currentGroup to newGroup would be a demotion.
     * Same level is NOT considered a demotion.
     */
    public static boolean isDemotion(GroupPath currentGroup, GroupPath newGroup){
        if(currentGroup == null || newGroup == null){
            return false;
        }
        return getHierarchyLevel(newGroup) < getHierarchyLevel(currentGroup);
    }


    /**
     * Checks if moving from currentGroup to newGroup is a valid transition.
     * Rules:
     * - Same level: ALLOWED (useful for updates without changes)
     * - One level up: ALLOWED (promotion)
     * - Two+ levels up: BLOCKED (skip levels not allowed)
     * - One level down: ALLOWED for superadmins only (handled in validation)
     * - Two+ levels down: BLOCKED (even for superadmins to prevent abuse)
     * - Special cases:
     *   - MODERATORS_PROFESSIONAL: Direct assignment allowed (explicit exception)
     *   - ADMINISTRATORS: Special handling (superadmin only)
     *   - SUPER_ADMINISTRATORS: Special handling (superadmin only)
     */
    public static boolean isValidTransition(GroupPath currentGroup, GroupPath newGroup){
        if(currentGroup == null || newGroup == null){
            return false;
        }

        // Same group is always valid (updates without changes)
        if(currentGroup == newGroup){
            return true;
        }

        // Special case: MODERATOR_PROFESSIONAL can be assigned directly
        if(newGroup == MODERATORS_PROFESSIONAL) {
            return true;
        }

        // Special case: ADMINISTRATORS AND SUPER_ADMINISTRATORS are handled by superadmin checks
        if(newGroup == ADMINISTRATORS || newGroup == SUPER_ADMINISTRATORS){
            return true;
        }

        int currentLevel = getHierarchyLevel(currentGroup);
        int newLevel = getHierarchyLevel(newGroup);

        // Can only move up ONE level up OR ONE level down
        // But down might be restricted by superadmin checks
        return Math.abs(newLevel - currentLevel) == 1;
    }

    /**
     * Checks if a user can be assigned to a group creation or reissue
     * Only MEMBERS_NEW and MODERATORS_PROFESSIONAL are allowed for new invitations.
     * */
    public static boolean isAllowedForCreationOrReissue(GroupPath group){
        return group == MEMBERS_NEW || group == MODERATORS_PROFESSIONAL;
    }

    
}




