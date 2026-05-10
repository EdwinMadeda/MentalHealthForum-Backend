package com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateThreadModeratorRequest {

    private ThreadType threadType;

    private ThreadStatus threadStatus;

    private Boolean isSticky;

    private Boolean isFeatured;

    private String lockReason;  // Optional reason for closing/locking

}
