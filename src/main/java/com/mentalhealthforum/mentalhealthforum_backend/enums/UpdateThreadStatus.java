package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum UpdateThreadStatus {
    OPEN,      // Active discussion, accepting new posts
    CLOSED,    // No longer accepting posts, but still visible
    ARCHIVED   // Old/inactive, hidden from main view but searchable
}
