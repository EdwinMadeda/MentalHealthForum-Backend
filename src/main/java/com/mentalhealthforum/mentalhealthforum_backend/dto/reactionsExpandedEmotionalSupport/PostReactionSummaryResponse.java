package com.mentalhealthforum.mentalhealthforum_backend.dto.reactionsExpandedEmotionalSupport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

/**
 * SUMMARY response for all reactions on a post.
 *
 * "Summary" means aggregated counts across reaction types - NO individual user data.
 * This intentionally does NOT include:
 * - List of users who reacted
 * - Timestamps of reactions
 * - Any identifying information
 *
 * Privacy is paramount in a mental health community.
 *
 * FUTURE EXTENSIBILITY (PRIVACY PRESERVING):
 * - Could add reaction removal count (for moderation)
 * - Could add most common reaction type
 * - Will NEVER add list of reactors without explicit user consent
 */
public class PostReactionSummaryResponse {
    private int totalReactionCount;
    private List<ReactionSummaryResponse> reactions;
}
