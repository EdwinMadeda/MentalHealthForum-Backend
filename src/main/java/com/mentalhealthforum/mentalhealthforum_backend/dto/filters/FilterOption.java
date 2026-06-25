package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FilterOption {
    private UUID id;
    private String label;
    private String value;
    private String avatarUrl;
    private Long count;

    public FilterOption(UUID id, String label, String value, Long count){
        this.id = id;
        this.label = label;
        this.value = value;
        this.count = count;
    }

    public FilterOption(UUID id, String label, String value, String avatarUrl, Long count){
        this.id = id;
        this.label = label;
        this.value = value;
        this.avatarUrl = avatarUrl;
        this.count = count;
    }

    public FilterOption(String label, String value, Long count){
        this.label = label;
        this.value = value;
        this.count = count;
    }
}
