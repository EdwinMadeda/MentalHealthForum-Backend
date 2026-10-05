package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ThreadStatus {
    OPEN("Open"),      // Active discussion, accepting new posts
    RESOLVED("Resolved"),  // Question answered or issue addressed (for QUESTION threads)
    CLOSED("Closed"),    // No longer accepting posts, but still visible
    ARCHIVED("Archived");   // Old/inactive, hidden from main view but searchable

    private final String displayName;

    ThreadStatus(String displayName) {
        this.displayName = displayName;
    }

    public static ThreadStatus fromString(String value){
        if(value == null){
            return null;
        }
        try {
            return ThreadStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
