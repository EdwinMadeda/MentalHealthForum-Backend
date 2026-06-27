package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    ACTION_TAKEN("Action Taken"),
    DISMISSED("Dismissed"),
    ESCALATED("Escalated");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }
}
