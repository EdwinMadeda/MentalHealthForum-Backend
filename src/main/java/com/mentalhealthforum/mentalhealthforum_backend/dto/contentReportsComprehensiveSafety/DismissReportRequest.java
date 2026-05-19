package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.DismissalReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DismissReportRequest(
        @NotNull(message = "Dismissal reason is required")
        DismissalReason reasonCode,

        @NotBlank(message = "Dismissal details are required")
        @Size(min = 10, message = "Dismissal details must be at least 10 characters")
        String details,

        String resolutionNotes
) {}
