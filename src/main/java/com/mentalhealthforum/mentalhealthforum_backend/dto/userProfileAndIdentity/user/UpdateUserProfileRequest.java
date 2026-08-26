package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ProfileVisibility;
import com.mentalhealthforum.mentalhealthforum_backend.service.OnboardingProfileData;
import com.mentalhealthforum.mentalhealthforum_backend.validation.ValidEmail;
import com.mentalhealthforum.mentalhealthforum_backend.validation.bio.ValidBio;
import com.mentalhealthforum.mentalhealthforum_backend.validation.displayName.ValidDisplayName;
import com.mentalhealthforum.mentalhealthforum_backend.validation.firstName.ValidFirstName;
import com.mentalhealthforum.mentalhealthforum_backend.validation.lastName.ValidLastName;
import com.mentalhealthforum.mentalhealthforum_backend.validation.timezone.ValidTimezone;
import com.mentalhealthforum.mentalhealthforum_backend.validation.url.ValidUrl;

import org.openapitools.jackson.nullable.JsonNullable;

@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public class UpdateUserProfileRequest implements OnboardingProfileData {

    // Getters and Setters
        @ValidEmail
        private JsonNullable<String> email = JsonNullable.undefined();

        @ValidFirstName
        private JsonNullable<String> firstName = JsonNullable.undefined();;

        @ValidLastName
        private JsonNullable<String> lastName = JsonNullable.undefined();;

        @ValidBio
        private JsonNullable<String> bio = JsonNullable.undefined();;

        @ValidDisplayName
        private JsonNullable<String> displayName = JsonNullable.undefined();;

        @ValidUrl
        private JsonNullable<String> avatarUrl = JsonNullable.undefined();;

        @ValidTimezone(nullable = true)
        private JsonNullable<String> timezone = JsonNullable.undefined();;

        private JsonNullable<ProfileVisibility> profileVisibility = JsonNullable.undefined();


        // Implement OnboardingProfileData interface getters matching your requirements
        @Override
        public String displayName() {
                return this.displayName.isPresent() ? this.displayName.get() : null;
        }

        @Override
        public String bio() {
                return this.bio.isPresent() ? this.bio.get() : null;
        }

        @Override
        public String timezone() {
                return this.timezone.isPresent() ? this.timezone.get() : null;
        }

        // --- Standard Getters & Setters for Jackson/Service layer ---
        public JsonNullable<String> getEmail() { return email; }
        public void setEmail(JsonNullable<String> email) { this.email = email; }

        public JsonNullable<String> getFirstName() { return firstName; }
        public void setFirstName(JsonNullable<String> firstName) { this.firstName = firstName; }

        public JsonNullable<String> getLastName() { return lastName; }
        public void setLastName(JsonNullable<String> lastName) { this.lastName = lastName; }

        public JsonNullable<String> getBio() { return bio; }
        public void setBio(JsonNullable<String> bio) { this.bio = bio; }

        public JsonNullable<String> getDisplayName() { return displayName; }
        public void setDisplayName(JsonNullable<String> displayName) { this.displayName = displayName; }

        public JsonNullable<String> getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(JsonNullable<String> avatarUrl) { this.avatarUrl = avatarUrl; }

        public JsonNullable<String> getTimezone() { return timezone; }
        public void setTimezone(JsonNullable<String> timezone) { this.timezone = timezone; }

        public JsonNullable<ProfileVisibility> getProfileVisibility() { return profileVisibility; }
        public void setProfileVisibility(JsonNullable<ProfileVisibility> profileVisibility) { this.profileVisibility = profileVisibility; }

}
