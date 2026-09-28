package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserHistoryFilterDto {
    private List<FilterOption> auditActions;
    private List<FilterOption> performedBys;
    private List<FilterOption> targetUsers;

}
