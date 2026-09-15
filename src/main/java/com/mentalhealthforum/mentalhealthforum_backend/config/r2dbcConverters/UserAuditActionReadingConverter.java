package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.UserAuditAction;

public class UserAuditActionReadingConverter extends AbstractPostgresEnumReadingConverter<UserAuditAction>{
    public UserAuditActionReadingConverter() {
        super(UserAuditAction.class);
    }
}
