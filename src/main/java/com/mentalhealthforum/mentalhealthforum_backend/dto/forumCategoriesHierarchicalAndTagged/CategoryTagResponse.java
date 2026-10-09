package com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CategoryTagResponse {

    // Tag subject attributes
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private Integer usage;
    private Instant createdAt;
    private Instant updatedAt;

    // Reference (nested)
    private UserDetails createdBy;

}
