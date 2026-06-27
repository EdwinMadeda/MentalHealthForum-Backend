package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryFilterDto {
    private List<FilterOption> tags;
    private List<FilterOption> parentCategories;
}
