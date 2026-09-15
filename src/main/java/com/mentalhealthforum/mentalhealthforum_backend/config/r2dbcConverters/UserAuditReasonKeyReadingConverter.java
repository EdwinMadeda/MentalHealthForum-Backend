package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditReasonKey;

public class UserAuditReasonKeyReadingConverter extends AbstractPostgresEnumReadingConverter<UserAuditReasonKey>{
    public UserAuditReasonKeyReadingConverter() {
        super(UserAuditReasonKey.class);
    }
}
