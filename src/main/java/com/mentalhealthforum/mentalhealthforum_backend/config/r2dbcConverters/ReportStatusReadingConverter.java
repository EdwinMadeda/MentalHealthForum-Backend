package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportStatus;

public class ReportStatusReadingConverter extends AbstractPostgresEnumReadingConverter<ReportStatus>{
    public ReportStatusReadingConverter() {
        super(ReportStatus.class);
    }
}
