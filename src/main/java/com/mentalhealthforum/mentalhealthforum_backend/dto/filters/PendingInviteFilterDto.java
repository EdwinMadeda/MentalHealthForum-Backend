package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PendingInviteFilterDto {
    private List<FilterOption> stages;
    private List<FilterOption> inviters;
    private List<FilterOption> groups;
}
