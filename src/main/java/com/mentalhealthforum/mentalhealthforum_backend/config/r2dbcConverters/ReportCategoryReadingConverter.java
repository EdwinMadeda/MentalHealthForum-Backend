package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;

public class ReportCategoryReadingConverter extends AbstractPostgresEnumReadingConverter<ReportCategory>{
    public ReportCategoryReadingConverter() {
        super(ReportCategory.class);
    }
}
