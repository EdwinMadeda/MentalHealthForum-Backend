package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportReasonCode;

public class ReportReasonCodeReadingConverter extends AbstractPostgresEnumReadingConverter<ReportReasonCode>{
    public ReportReasonCodeReadingConverter() {
        super(ReportReasonCode.class);
    }
}
