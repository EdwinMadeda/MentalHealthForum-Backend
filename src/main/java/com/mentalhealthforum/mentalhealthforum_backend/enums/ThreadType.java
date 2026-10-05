package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ThreadType {
    DISCUSSION("Discussion"),               // General conversation, no specific outcome expected
    QUESTION("Question"),                   // Seeking specific answers/advice
    CRISIS_SUPPORT("Crisis Support"),       // Urgent support needed
    PEER_REVIEW("Peer Review"),             // Sharing for feedback from peers
    POLL("Poll");                           // Community poll/survey

    private final String displayName;

    ThreadType(String displayName) {
        this.displayName = displayName;
    }

    public static ThreadType fromString(String value){
        if(value == null){
            return null;
        }
        try {
            return ThreadType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
