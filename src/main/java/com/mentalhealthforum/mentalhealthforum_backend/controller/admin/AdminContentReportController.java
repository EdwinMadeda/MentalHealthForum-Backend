package com.mentalhealthforum.mentalhealthforum_backend.controller.admin;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety.ReportResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety.ResolveReportRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety.UpdateReportDetailsRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety.UserReportHistoryResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.ReportService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/moderator/forum/content-reports")
public class AdminContentReportController {

    private final ReportService reportService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public AdminContentReportController(
            ReportService reportService,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.reportService = reportService;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // TODO: Future - Admin ban functionality
    // POST /api/admin/reports/users/{userId}/ban - Ban user from submitting reports
    // DELETE /api/admin/reports/users/{userId}/ban - Remove ban
    // GET /api/admin/reports/users/{userId}/ban-status - Check ban status

}
