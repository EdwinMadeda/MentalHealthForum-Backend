package com.mentalhealthforum.mentalhealthforum_backend.dto.novu;

public record NovuSubscriberData(
    String type,      // "pending-registration" | "active-app-user" | "pending-admin-invite"
    String status,    // "unverified" | "verified"
    String source     // "self_registration" | "keycloak" | "app-user"
) {
    public static NovuSubscriberData forUserInStaging(){
        return new NovuSubscriberData("pending-registration", "unverified", "self-registration");
    }

    public static NovuSubscriberData forNewUserInKeycloak(){
        return new NovuSubscriberData("new-user-in-keycloak", "verified", "keycloak");
    }

    public static NovuSubscriberData forAppUser(){
        return new NovuSubscriberData("active-app-user", "verified", "app-user");
    }
}
