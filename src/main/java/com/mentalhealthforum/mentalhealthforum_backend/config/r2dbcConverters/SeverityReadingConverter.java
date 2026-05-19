package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;

public class SeverityReadingConverter extends AbstractPostgresEnumReadingConverter<Severity>{
    public SeverityReadingConverter() {
        super(Severity.class);
    }
}
