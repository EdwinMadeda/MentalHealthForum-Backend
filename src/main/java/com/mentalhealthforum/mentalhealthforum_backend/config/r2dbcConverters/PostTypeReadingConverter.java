package com.mentalhealthforum.mentalhealthforum_backend.config.r2dbcConverters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.PostType;

public class PostTypeReadingConverter extends AbstractPostgresEnumReadingConverter<PostType>{
    public PostTypeReadingConverter() {
        super(PostType.class);
    }
}
