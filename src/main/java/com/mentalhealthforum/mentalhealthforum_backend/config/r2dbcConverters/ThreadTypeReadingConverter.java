package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;

public class ThreadTypeReadingConverter extends AbstractPostgresEnumReadingConverter<ThreadType>{
    public ThreadTypeReadingConverter() {
        super(ThreadType.class);
    }
}
