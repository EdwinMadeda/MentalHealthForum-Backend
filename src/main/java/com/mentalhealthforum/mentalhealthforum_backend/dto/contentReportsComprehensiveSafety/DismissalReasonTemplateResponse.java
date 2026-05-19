package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.DismissalReason;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DismissalReasonTemplateResponse {

    private DismissalReason reasonCode;
    private String defaultMessage;
    private String description;
    private String exampleMessage;

}
