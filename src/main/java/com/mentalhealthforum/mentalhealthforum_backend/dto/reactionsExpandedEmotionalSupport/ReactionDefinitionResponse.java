package com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDefinitionResponse {

    private ReactionType reactionType;
    private String displayName;
    private String iconClass;
    private String description;
    private int sortOrder;

}
