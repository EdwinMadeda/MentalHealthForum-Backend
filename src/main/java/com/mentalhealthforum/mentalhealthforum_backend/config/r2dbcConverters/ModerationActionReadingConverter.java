package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;

public class ModerationActionReadingConverter extends AbstractPostgresEnumReadingConverter<ModerationAction>{
    public ModerationActionReadingConverter() {
        super(ModerationAction.class);
    }
}
