package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@Getter
public enum RealmRole {
    FORUM_MEMBER("forum_member", "Basic forum access", "Forum Member"),
    TRUSTED_MEMBER("trusted_member", "Trusted community member with additional privileges", "Trusted Member"),
    PEER_SUPPORTER("peer_supporter", "Can provide peer support", "Peer Supporter"),
    MODERATOR("moderator", "Content moderation capabilities", "Moderator"),
    ADMIN("admin", "Full administrative access", "Admin"),
    SUPER_ADMIN("super_admin", "Super administrator with system management privileges", "Super Admin");

    private final String roleName;
    private final String roleDescription;
    private final String displayName;

    RealmRole(String roleName, String roleDescription, String displayName) {
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.displayName = displayName;
    }

    public static RealmRole fromRoleName(String roleName){
        for(RealmRole role: values()){
            if(role.roleName.equals(roleName)){
                return role;
            }
        }
        return null;
    }

    // Converts a collection of realmRole names to an array of realmRole enums
    public static RealmRole[] fromRoleNames(Collection<String> roleNames){
        if(roleNames == null || roleNames.isEmpty()){
            return new RealmRole[0];
        }

        return roleNames.stream()
                .map(RealmRole::fromRoleName)
                .filter(Objects::nonNull)
                .toArray(RealmRole[]::new);
    }

    public static String[] toRoleNames(RealmRole[] roles){
        if(roles == null || roles.length == 0){
            return null;
        }

         return Arrays.stream(roles)
                 .map(RealmRole::getRoleName)
                 .toArray(String[]::new);

    }

    public static boolean isValidRole(String roleName){
        return fromRoleName(roleName) != null;
    }
}
