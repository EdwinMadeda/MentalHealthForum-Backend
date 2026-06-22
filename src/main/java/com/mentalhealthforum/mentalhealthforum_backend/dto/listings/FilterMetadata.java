package com.mentalhealthforum.mentalhealthforum_backend.dto.listings;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FilterMetadata<T> {
    private T filters;
    private List<SortOption> sortOptions;
}
