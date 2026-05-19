package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.DismissalReason;

public class DismissalReasonReadingConverter extends AbstractPostgresEnumReadingConverter<DismissalReason>{
    public DismissalReasonReadingConverter() {
        super(DismissalReason.class);
    }
}
