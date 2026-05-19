package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
import com.mentalhealthforum.mentalhealthforum_backend.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportDetailsRequest {

    private Severity severity; // Change severity level
    private String resolutionNotes; // Add/update resolution notes
    private ModerationAction actionTaken; // Update action taken
    private String actionTakenDetails; // Update action details
    // All fields optional - only update what's provided

}
