package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.DismissalReason;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
import com.mentalhealthforum.mentalhealthforum_backend.model.DismissalReasonTemplateEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DismissalReasonTemplateRepository extends R2dbcRepository<DismissalReasonTemplateEntity, UUID> {

    Flux<DismissalReasonTemplateEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    Mono<DismissalReasonTemplateEntity> findByReasonCode(DismissalReason reasonCode);

    @Query("SELECT * FROM dismissal_reason_templates WHERE is_active = true ORDER by display_order")
    Flux<DismissalReasonTemplateEntity> findAllActive();

}
