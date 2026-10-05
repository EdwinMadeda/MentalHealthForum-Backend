package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ReportTargetType {
    THREAD("Thread"),
    POST("Post"),
    USER("User");

    private final String displayName;

    ReportTargetType(String displayName) {
        this.displayName = displayName;
    }
}
