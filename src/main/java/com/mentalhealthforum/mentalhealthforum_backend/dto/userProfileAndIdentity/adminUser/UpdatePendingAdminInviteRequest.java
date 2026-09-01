package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.validation.group.ValidAssignableGroup;
import org.openapitools.jackson.nullable.JsonNullable;

@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public class UpdatePendingAdminInviteRequest {

        @ValidAssignableGroup
        private JsonNullable<GroupPath> group = JsonNullable.undefined();

        private JsonNullable<Boolean> isEnabled = JsonNullable.undefined();

        public JsonNullable<GroupPath> getGroup(){
                return group;
        }

        public void setGroup(JsonNullable<GroupPath> group){
                this.group = group;
        }

        public JsonNullable<Boolean> getIsEnabled() {
                return isEnabled;
        }

        public void setIsEnabled(JsonNullable<Boolean> isEnabled){
                this.isEnabled = isEnabled;
        }
}
