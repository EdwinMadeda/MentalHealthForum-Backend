package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum Severity {
    LOW("Low"),             // Minor issue, low priority
    MEDIUM("Medium"),       // Needs attention, standard priority
    HIGH("High"),           // Serious concern, escalate
    CRITICAL("Critical");   // Immediate danger, alert all mods

    private final String displayName;

    Severity( String displayName) {
        this.displayName = displayName;
    }
}
