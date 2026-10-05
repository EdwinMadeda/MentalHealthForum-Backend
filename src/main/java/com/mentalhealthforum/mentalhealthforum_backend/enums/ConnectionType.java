package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ConnectionType {
    INCOMING("Incoming"),  // initiated_by != current user
    OUTGOING("Outgoing"),  // initiated_by == current user
    ALL("All");            // no filter on initiated_by

    private final String displayName;

    ConnectionType(String displayName) {
        this.displayName = displayName;
    }
}
