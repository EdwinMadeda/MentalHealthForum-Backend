package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum CategorySortField {
    SORT_ORDER("sort_order", "sort order", "ASC"),
    NAME("name", "name", "ASC"),
    CREATED_AT("created_at", "created at", "DESC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    CategorySortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static final CategorySortField DEFAULT = CategorySortField.SORT_ORDER;

    public static CategorySortField fromString(String value) {
        if(value == null){
            return DEFAULT;  // Default to sort_order
        }
        for(CategorySortField field : CategorySortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  DEFAULT;
    }

    public String determineSortDirection(String sortDirection) {
        if(sortDirection != null){
            return "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
        }
        return this.defaultDirection;
    }

    public SortOption toSortOption(){
        return SortOption.builder()
                .value(this.name())
                .label(this.label)
                .defaultDirection(this.defaultDirection)
                .isDefault(this == DEFAULT)
                .build();
    }

}
