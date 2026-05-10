package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;

public class ThreadStatusReadingConverter extends AbstractPostgresEnumReadingConverter<ThreadStatus>{
    public ThreadStatusReadingConverter() {
        super(ThreadStatus.class);
    }
}
