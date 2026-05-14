package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReactionType;
import com.mentalhealthforum.mentalhealthforum_backend.model.ReactionDefinitionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ReactionDefinitionRepository extends R2dbcRepository<ReactionDefinitionEntity,ReactionType> {

    Flux<ReactionDefinitionEntity> findAllByOrderBySortOrderAsc();
    Mono<ReactionDefinitionEntity> findByReactionType(ReactionType reactionType);

}
