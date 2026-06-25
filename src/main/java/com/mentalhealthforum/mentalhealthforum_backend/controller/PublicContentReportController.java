package com.mentalhealthforum.mentalhealthforum_backend.controller;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportTargetType;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.ReportService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/forum/content-reports")
public class PublicContentReportController {

    private final ReportService reportService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public PublicContentReportController(
            ReportService reportService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.reportService = reportService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // ==================== REPORT ACTIONS ====================

    @PostMapping("/threads")
    public Mono<ResponseEntity<StandardSuccessResponse<ReportResponse>>> createThreadReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateThreadReportRequest request
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.createThreadReport(request, viewerContext)
                .map(report-> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("Thread report submitted successfully", report)));
    }

    @PostMapping("/posts")
    public Mono<ResponseEntity<StandardSuccessResponse<ReportResponse>>> createPostReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePostReportRequest request
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.createPostReport(request, viewerContext)
                .map(report-> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("Post report submitted successfully", report)));
    }

    @PostMapping("/users")
    public Mono<ResponseEntity<StandardSuccessResponse<ReportResponse>>> createUserReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserReportRequest request
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.createUserReport(request, viewerContext)
                .map(report-> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new StandardSuccessResponse<>("User report submitted successfully", report)));
    }

    @GetMapping
    public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<ReportResponse>>>> getOwnReports(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "target_type") ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(defaultValue = "", name = "search") @Parameter(description = "Search by reason or details") String search,
            @RequestParam(defaultValue = "reported_at", name = "sort_by") @Parameter(description = "Sort by: severity, reported_at") String sortBy,
            @RequestParam(required = false, name = "sort_direction") @Parameter(description = "Sort direction: asc or desc") String sortDirection
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.getOwnReports(page, size, targetType, status, category, search, sortBy, sortDirection, viewerContext)
                .map(reports->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Content reports retrieved successfully", reports)));
    }

    @GetMapping("/{reportId}")
    public Mono<ResponseEntity<StandardSuccessResponse<ReportResponse>>> getReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reportId
            ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.getReportById(reportId, viewerContext)
                .map(report->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Content report retrieved successfully", report)));
    }

    // ==================== REPORT HISTORY ====================

    @GetMapping("/history/me")
    public Mono<ResponseEntity<StandardSuccessResponse<UserReportHistoryResponse>>> getOwnReportHistory(
            @AuthenticationPrincipal Jwt jwt
    ){

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        return reportService.getOwnReportHistory(viewerContext)
                .map(history ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Content report history retrieved successfully", history)));
    }

    // ==================== REFERENCE DATA ====================

    @GetMapping("/templates")
    public Mono<ResponseEntity<StandardSuccessResponse<List<ReportTemplateResponse>>>> getReportTemplates(){

        return reportService.getReportTemplates()
                .collectList()
                .map(templates ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Report templates retrieved successfully", templates)));
    }

    @GetMapping("/templates/category/{category}")
    public Mono<ResponseEntity<StandardSuccessResponse<List<ReportTemplateResponse>>>> getReportTemplatesByCategory(
            @PathVariable ReportCategory category
    ){

        return reportService.getReportTemplatesByCategory(category)
                .collectList()
                .map(templates ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("Report templates retrieved successfully", templates)));
    }

}
