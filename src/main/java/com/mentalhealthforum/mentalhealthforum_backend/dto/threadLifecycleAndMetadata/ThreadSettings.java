package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThreadSettings {

    @Builder.Default
    private Instant  autoLockAt = null; //Scheduled auto-lock date

    @Builder.Default
    private Instant scheduledPostAt = null; // Future publish date

    @Builder.Default
    private String customerReminder = null; // Custom not for thread
}
