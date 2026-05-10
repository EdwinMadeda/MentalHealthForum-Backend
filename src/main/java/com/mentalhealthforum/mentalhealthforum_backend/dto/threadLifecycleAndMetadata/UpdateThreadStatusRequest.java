package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UpdateThreadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateThreadStatusRequest(
        @NotNull(message = "Thread Status is required")
        UpdateThreadStatus threadStatus
) {
    public ThreadStatus toThreadStatus(){
        return switch (threadStatus){
                case OPEN -> ThreadStatus.OPEN;
                case CLOSED -> ThreadStatus.CLOSED;
                case ARCHIVED -> ThreadStatus.ARCHIVED;
        };
    }
}
