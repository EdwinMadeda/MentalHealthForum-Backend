package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserFilterDto {
    private List<FilterOption> roles;
    private List<FilterOption> groups;

}
