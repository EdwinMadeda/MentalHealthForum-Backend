package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum PostType {
    REPLY,            // Standard user response
    ANSWER,           // Answer to a QUESTION thread (potential best answer)
    SYSTEM_MESSAGE,   // Auto-generated system message
    MODERATOR_NOTE    // Official moderator communication
}
