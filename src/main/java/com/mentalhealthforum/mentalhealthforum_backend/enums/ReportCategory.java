package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum ReportCategory  {
    SPAM,               // Promotional content, off-topic
    HARASSMENT,         // Targeting/bullying another user
    SELF_HARM,          // Content about self-harm
    SUICIDE,            // Content about suicide
    VIOLENCE,           // Threats or violent content
    MISINFORMATION,     // Dangerous medical/mental health misinformation
    PRIVACY_VIOLATION,  // Sharing someone's personal info
    INAPPROPRIATE,      // Generally inappropriate content
    OTHER               // Requires manual review
}
