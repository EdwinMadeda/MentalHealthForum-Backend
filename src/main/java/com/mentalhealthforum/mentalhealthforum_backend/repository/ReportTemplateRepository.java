package com.mentalhealthforum.mentalhealthforum_backend.repository;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.model.ReportTemplateEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ReportTemplateRepository extends R2dbcRepository<ReportTemplateEntity, UUID> {

    Flux<ReportTemplateEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    Flux<ReportTemplateEntity> findByReportCategoryOrderByDisplayOrderAsc(ReportCategory category);

    Flux<ReportTemplateEntity> findByReportCategoryAndIsActiveTrueOrderByDisplayOrderAsc(ReportCategory reportCategory);

}
