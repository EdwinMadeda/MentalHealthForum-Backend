package com.mentalhealthforum.mentalhealthforum_backend.dto.contentReportsComprehensiveSafety;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ModerationAction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModerationActionTemplateResponse {

    private ModerationAction actionType;
    private String defaultMessage;
    private String description;
    private String exampleMessage;

}
