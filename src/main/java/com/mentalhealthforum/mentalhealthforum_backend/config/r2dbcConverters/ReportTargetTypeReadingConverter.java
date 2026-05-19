package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportTargetType;

public class ReportTargetTypeReadingConverter extends AbstractPostgresEnumReadingConverter<ReportTargetType>{
    public ReportTargetTypeReadingConverter() {
        super(ReportTargetType.class);
    }
}