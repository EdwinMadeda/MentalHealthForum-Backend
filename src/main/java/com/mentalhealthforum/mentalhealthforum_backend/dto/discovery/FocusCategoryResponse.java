package com.mentalhealthforum.mentalhealthforum_backend.dto.discovery;

import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class FocusCategoryResponse {

    // Focus metadata (subject)
    private UUID id;
    private Boolean notificationEnabled;
    private Instant focusedAt;

    // Category core info
    private CategoryDetails category;

    // Category metadata
//    private CategoryDetails parentCategory;
    private Integer threadCount; // How many threads in this category

}
