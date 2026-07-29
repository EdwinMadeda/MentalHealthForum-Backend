package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.AccountStatus;

public class AccountStatusReadingConverter extends AbstractPostgresEnumReadingConverter<AccountStatus>{
    public AccountStatusReadingConverter() {
        super(AccountStatus.class);
    }
}
