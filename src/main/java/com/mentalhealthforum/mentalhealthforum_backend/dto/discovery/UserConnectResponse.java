package com.mentalhealthforum.mentalhealthforum_backend.dto.discovery;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ConnectionStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ConnectionType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserConnectResponse {

    // Connection metadata
    private UUID id;
    private ConnectionStatus connectionStatus;
    private Boolean notificationEnabled;
    private Instant createdAt;

    // Initiator (who sent the request)
    private UserDetails initiatedBy;

    // Recipient (who received the request)
    private UserDetails recipient;

    // Viewer perspective
    private ConnectionType connectionType;

}
