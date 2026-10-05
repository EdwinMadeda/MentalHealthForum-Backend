package com.mentalhealthforum.mentalhealthforum_backend.enums;

import lombok.Getter;

@Getter
public enum ReportCategory  {
    SPAM("Spam"),                               // Promotional content, off-topic
    HARASSMENT("Harassment"),                   // Targeting/bullying another user
    SELF_HARM("Self harm"),                     // Content about self-harm
    SUICIDE("Suicide"),                         // Content about suicide
    VIOLENCE("Violence"),                       // Threats or violent content
    MISINFORMATION("Misinformation"),           // Dangerous medical/mental health misinformation
    PRIVACY_VIOLATION("Privacy violation"),     // Sharing someone's personal info
    INAPPROPRIATE("Inappropiate"),              // Generally inappropriate content
    OTHER("Other")                              // Requires manual review
    ;

    private final String displayName;

    ReportCategory(String displayName) {
        this.displayName = displayName;
    }
}
