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

/**
 * SUMMARY response for a single reaction type on a post.
 *
 * "Summary" means aggregated counts only - NO individual user data is exposed.
 * This is intentional for privacy in a mental health community.
 *
 * Current includes: count, reaction type metadata, and whether current user reacted.
 *
 * FUTURE EXTENSIBILITY (PRIVACY PRESERVING):
 * - Could add timestamp of current user's reaction only
 * - Could indicate if reaction can be removed (time window based)
 * - Will NEVER include list of users who reacted
 */
public class ReactionSummaryResponse {

    private ReactionType reactionType;
    private String displayName;
    private String iconClass;
    private int count;

    /**
     * Whether the currently authenticated user has reacted with this type.
     * This is the ONLY user-specific data returned - and only about the requesting user.
     */
    private boolean userReacted;

}
