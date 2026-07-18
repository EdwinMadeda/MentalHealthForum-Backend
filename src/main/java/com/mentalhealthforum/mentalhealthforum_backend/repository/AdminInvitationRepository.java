package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser.PendingAdminInviteDto;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OnboardingStage;
import com.mentalhealthforum.mentalhealthforum_backend.model.AdminInvitationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AdminInvitationRepository extends R2dbcRepository<AdminInvitationEntity, UUID> {

    // Simple lookup to see if someone is already in the "Lobby"
    Mono<AdminInvitationEntity> findByKeycloakId(UUID keycloakId);

    Mono<Boolean> existsByKeycloakId(UUID keycloakId);

    Mono<AdminInvitationEntity> findByEmail(String email);

    @Query("""
        SELECT i.keycloak_id AS user_id,
               i.username,
               i.first_name,
               i.last_name,
               i.email,
               i.groups,
               i.is_enabled,
               i.is_email_verified,
               i.invited_by,
               u.display_name AS invited_by_display_name,
               u.avatar_url AS invited_by_avatar_url,
               i.date_created,
               i.updated_at,
               i.current_stage
        FROM admin_invitations i
        LEFT join app_users u ON i.invited_by = u.keycloak_id
        WHERE (:search IS NULL
    
            -- simple unaccent
            OR to_tsvector('public.simple_unaccent',
                coalesce(i.email, '') || ' ' ||
                coalesce(i.username, '') || ' ' ||
                coalesce(i.first_name, '') || ' ' ||
                coalesce(i.last_name, '')
            ) @@ websearch_to_tsquery('public.simple_unaccent', :search)
    
            -- trigram fallback
    
            OR public.unaccent_immutable(i.email) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.username) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.first_name) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.last_name) % public.unaccent_immutable(:search)
    
            )
    
            AND (:groups IS NULL OR i.groups && :groups)
            AND (:invitedByUserId IS NULL OR i.invited_by = :invitedByUserId)
            AND (:onboardingStage IS NULL OR i.current_stage = :onboardingStage::onboarding_stage_enum)
    
            ORDER BY
    
                -- DESC
                CASE :sortDirection
                    WHEN 'DESC' THEN
                        CASE :sortBy
                            WHEN 'email' THEN i.email
                            WHEN 'username' THEN i.username
                            ELSE i.date_created::text
                        END
                    ELSE NULL
                END DESC NULLS LAST,

                -- ASC
                CASE :sortDirection
                    WHEN 'ASC' THEN
                        CASE :sortBy
                            WHEN 'email' THEN i.email
                            WHEN 'username' THEN i.username
                            ELSE i.date_created::text
                        END
                    ELSE NULL
                END ASC NULLS FIRST,
    
                -- 4. Tie breaker for deterministic ordering
                u.keycloak_id
            LIMIT :limit OFFSET :offset;
    """)
    Flux<PendingAdminInviteDto> findPendingInvitesPaginated(
        @Param("invitedByUserId") UUID invitedByUserId,
        @Param("groups") String[] groups,
        @Param("onboardingStage") String onboardingStage,
        @Param("search") String search,
        @Param("sortBy") String sortBy,
        @Param("sortDirection") String sortDirection,
        @Param("limit") int limit,
        @Param("offset") int offset
    );


    @Query("""
        SELECT COUNT(*)
        FROM admin_invitations i
        WHERE (:search IS NULL
    
            OR to_tsvector('public.simple_unaccent',
                coalesce(i.email, '') || ' ' ||
                coalesce(i.username, '') || ' ' ||
                coalesce(i.first_name, '') || ' ' ||
                coalesce(i.last_name, '')
            ) @@ websearch_to_tsquery('public.simple_unaccent', :search)
    
            OR public.unaccent_immutable(i.email) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.username) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.first_name) % public.unaccent_immutable(:search)
            OR public.unaccent_immutable(i.last_name) % public.unaccent_immutable(:search)
    
            )
    
            AND (:groups IS NULL OR i.groups && :groups)
            AND (:invitedByUserId IS NULL OR i.invited_by = :invitedByUserId)
            AND (:onboardingStage IS NULL OR i.current_stage = :onboardingStage::onboarding_stage_enum)
    """)
    Mono<Long> countPendingInvitesWithFilters(
            @Param("invitedByUserId") UUID invitedByUserId,
            @Param("groups") String[] groups,
            @Param("onboardingStage") String onboardingStage,
            @Param("search") String search
    );


    @Modifying
    @Query("""
            UPDATE admin_invitations
            SET current_stage = 'AWAITING_PASSWORD_RESET',
                is_email_verified = TRUE,
                updated_at = NOW()
                WHERE keycloak_id = :keycloakId AND current_stage = 'AWAITING_VERIFICATION'
            """)
    Mono<Integer> markEmailVerifiedAndAdvanceStage(UUID keycloakId);


    @Modifying
    @Query("""
        UPDATE admin_invitations
        SET current_stage = :newStage,
            updated_at = NOW()
            WHERE keycloak_id = :keycloakId
    """)
    Mono<Integer> updateStage(UUID keycloakId, OnboardingStage newStage);

    @Modifying
    @Query("""
            UPDATE admin_invitations
            SET is_initial_login = FALSE
                WHERE keycloak_id = :keycloakId AND is_initial_login = TRUE
            """)
    Mono<Integer> invalidateOneTimePass(UUID keycloakId);

    // Clean up once the user is synced to app_users
    Mono<Void> deleteByKeycloakId(UUID keycloakId);
}
