package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SortOption {
    private String value;
    private String label;
    private String defaultDirection;
}
