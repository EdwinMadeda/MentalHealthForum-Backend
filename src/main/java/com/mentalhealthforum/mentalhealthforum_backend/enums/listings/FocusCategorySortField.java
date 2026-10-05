package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum FocusCategorySortField {
    CREATED_AT("created_at", "created at", "DESC"),
    CATEGORY_NAME("category_name", "category name", "ASC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    FocusCategorySortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static final FocusCategorySortField DEFAULT = FocusCategorySortField.CREATED_AT;

    public static FocusCategorySortField fromString(String value) {
        if(value == null){
            return DEFAULT;  // Default to created_at
        }
        for(FocusCategorySortField field : FocusCategorySortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return DEFAULT;
    }

    public String determineSortDirection(String sortDirection) {
        if(sortDirection != null){
            return "desc".equalsIgnoreCase(sortDirection)? "DESC" : "ASC";
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
