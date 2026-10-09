package com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryDetails {
    private UUID id;
    private String slug;
    private String name;
    private String description;
    private String colorTheme;
    private ContentWarningType contentWarningType;
    private Boolean isParent;
    private Boolean isChild;
}
