package com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record AddReactionRequest(
        @NotNull(message = "Reaction type is required")
        ReactionType reactionType
) {}
