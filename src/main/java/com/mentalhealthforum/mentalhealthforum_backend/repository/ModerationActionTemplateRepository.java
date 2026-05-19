package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
import com.mentalhealthforum.mentalhealthforum_backend.model.ModerationActionTemplateEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ModerationActionTemplateRepository extends R2dbcRepository<ModerationActionTemplateEntity, UUID> {

    Flux<ModerationActionTemplateEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    Mono<ModerationActionTemplateEntity> findByActionType(ModerationAction actionType);

    @Query("SELECT * FROM moderation_action_templates WHERE is_active = true ORDER by display_order")
    Flux<ModerationActionTemplateEntity> findAllActive();

}
