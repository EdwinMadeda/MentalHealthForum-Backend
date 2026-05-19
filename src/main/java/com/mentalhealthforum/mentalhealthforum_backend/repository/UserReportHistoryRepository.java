package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.model.UserReportHistoryEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserReportHistoryRepository extends R2dbcRepository<UserReportHistoryEntity, UUID> {

    Mono<UserReportHistoryEntity> findByUserId(UUID userId);
}
