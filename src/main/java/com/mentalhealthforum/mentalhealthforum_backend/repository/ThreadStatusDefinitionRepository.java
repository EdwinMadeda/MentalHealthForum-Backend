package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadStatusDefinitionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ThreadStatusDefinitionRepository extends ReactiveCrudRepository<ThreadStatusDefinitionEntity, ThreadStatus> {

    @Query("SELECT * FROM thread_status_definitions ORDER BY display_name ASC")
    Flux<ThreadStatusDefinitionEntity> findAllByOrderByDisplayNameASC();

    Flux<ThreadStatusDefinitionEntity> findByUserVisibleTrue();

    Flux<ThreadStatusDefinitionEntity> findByThreadStatus(ThreadStatus threadStatus);
}
