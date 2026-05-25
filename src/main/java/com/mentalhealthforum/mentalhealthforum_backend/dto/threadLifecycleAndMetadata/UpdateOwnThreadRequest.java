package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.EditReason;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOwnThreadRequest {

    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @NotNull(message = "Edit reason is required")
    private EditReason editReason;

    @Size(max = 500, message = "Custom reason cannot exceed 500 characters")
    private String editReasonCustomText; // Required when editReason = OTHER

    private ContentWarningType contentWarningType;

    private String contentWarningCustomText;

    private List<String> tags;

}
