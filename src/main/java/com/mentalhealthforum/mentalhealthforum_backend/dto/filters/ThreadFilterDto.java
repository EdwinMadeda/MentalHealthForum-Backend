package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ThreadFilterDto {
    private List<FilterOption> creators;
    private List<FilterOption> categories;
    private List<FilterOption> tags;
}
