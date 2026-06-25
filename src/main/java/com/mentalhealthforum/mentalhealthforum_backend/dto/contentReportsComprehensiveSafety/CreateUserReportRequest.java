package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportCategory;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ReportTargetType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserReportRequest {

    @NotNull(message = "Reported user Id is required")
    private UUID reportedUserId;

    @NotNull(message = "Report category is required")
    private ReportCategory reportCategory;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String details;

    @Builder.Default
    private Boolean isAnonymous = false;

}
