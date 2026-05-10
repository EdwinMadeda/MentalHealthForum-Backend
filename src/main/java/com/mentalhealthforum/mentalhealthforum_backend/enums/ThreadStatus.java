package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum ThreadStatus {
    OPEN,      // Active discussion, accepting new posts
    RESOLVED,  // Question answered or issue addressed (for QUESTION threads)
    CLOSED,    // No longer accepting posts, but still visible
    ARCHIVED   // Old/inactive, hidden from main view but searchable
}
