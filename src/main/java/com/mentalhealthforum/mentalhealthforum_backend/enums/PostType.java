package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum PostType {
    REPLY("Reply"),                         // Standard user response
    ANSWER("Answer"),                       // Answer to a QUESTION thread (potential best answer)
    SYSTEM_MESSAGE("System message"),       // Auto-generated system message
    MODERATOR_NOTE("Moderator note");       // Official moderator communication

    private final String displayName;

    PostType(String displayName) {
        this.displayName = displayName;
    }
}
