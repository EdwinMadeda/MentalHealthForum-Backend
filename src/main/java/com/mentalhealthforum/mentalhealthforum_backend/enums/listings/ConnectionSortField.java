package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum ConnectionSortField {
    CREATED_AT("created_at", "created at", "DESC"),
    DISPLAY_NAME("display_name", "display name", "ASC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    ConnectionSortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static final ConnectionSortField DEFAULT = ConnectionSortField.CREATED_AT;

    public static ConnectionSortField fromString(String value) {
        if(value == null){
            return DEFAULT;  // Default to created_at
        }
        for(ConnectionSortField field : ConnectionSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  DEFAULT;
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
