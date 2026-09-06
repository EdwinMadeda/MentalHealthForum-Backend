package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;

public record AvailableGroup(
        String value,        // "MEMBERS_NEW"
        String path,         // "/members/new"
        String displayName   // "New members"
) {
    public AvailableGroup(GroupPath groupPath){
        this (
                groupPath.name(),
                groupPath.getPath(),
                groupPath.getDisplayName()
        );
    }
}
