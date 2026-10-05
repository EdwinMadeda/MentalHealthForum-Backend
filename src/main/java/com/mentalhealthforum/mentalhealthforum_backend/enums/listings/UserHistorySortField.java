package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum UserHistorySortField {
    DATE_CREATED("created_at", "date created", "DESC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    UserHistorySortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static final UserHistorySortField DEFAULT = UserHistorySortField.DATE_CREATED;

    public static UserHistorySortField fromString(String value) {
        if(value == null){
            return DEFAULT;
        }
        for(UserHistorySortField field : UserHistorySortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return DEFAULT;
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
