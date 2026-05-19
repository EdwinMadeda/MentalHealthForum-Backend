package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateReportRequest(
    @NotBlank(message = "Escalation reason is required")
    @Size(min = 10, message = "Escalation reason must be at least 10 characters")
    String reason,

    @Size(max = 500, message = "Resolution notes cannot exceed 500 characters")
    String resolutionNotes
) {}
