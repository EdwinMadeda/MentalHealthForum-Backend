package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.listings.SortOption;
import lombok.Getter;

@Getter
public enum ThreadSortField {
    CREATED_AT("created_at", "created at", "DESC"),
    LAST_ACTIVITY_AT("last_activity_at", "last activity at", "DESC"),
    POST_COUNT("post_count", "post count", "DESC"),
    VIEW_COUNT("view_count", "view count", "DESC"),
    TITLE("title", "title", "ASC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    ThreadSortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static ThreadSortField fromString(String value) {
        if(value == null){
            return LAST_ACTIVITY_AT;  // Default to most recent activity
        }
        for(ThreadSortField field : ThreadSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  LAST_ACTIVITY_AT;
    }

    public String determineSortDirection(String sortDirection){
        if(sortDirection != null){
            return "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
        }
        return this.defaultDirection;
    }

    public SortOption toSortOption(){
        return SortOption.builder()
                .value(this.value)
                .label(this.label)
                .defaultDirection(this.defaultDirection)
                .build();
    }

}
