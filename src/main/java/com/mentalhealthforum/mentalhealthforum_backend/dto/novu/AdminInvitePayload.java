package com.mentalhealthforum.mentalhealthforum_backend.dto.novu;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.service.NovuPayload;

import java.util.Map;

public record AdminInvitePayload(
        String firstName,
        String temporaryPassword,
        String invitationLink,
        String groupPath
) implements NovuPayload {
    @Override
    public Map<String, Object> toPayloadMap() {

        String friendlyGroupName = GroupPath.getFriendlyName(groupPath);

        return Map.of(
                "firstName", firstName,
                "temporaryPassword", temporaryPassword,
                "invitationLink", invitationLink,
                "groupName", friendlyGroupName
        );
    }
}
