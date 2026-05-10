package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.EditReason;

public class EditReasonReadingConverter extends AbstractPostgresEnumReadingConverter<EditReason>{
    public EditReasonReadingConverter() {
        super(EditReason.class);
    }
}
