package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum AppUserSortField {
    DISPLAY_NAME("display_name", "display name", "ASC"),
    DATE_JOINED("date_joined", "date joined", "DESC"),
    POST_COUNT("posts_count", "posts count", "DESC"),
    REPUTATION_SCORE("reputation_score", "reputation score", "DESC"),
    LAST_POSTED_AT("last_posted_at", "last posted at", "DESC"),
    LAST_ACTIVITY_AT("last_active_at", "last active at", "DESC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    AppUserSortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static AppUserSortField fromString(String value) {
        if(value == null){
            return DISPLAY_NAME;  // Default to alphabetical
        }
        for(AppUserSortField field : AppUserSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  DISPLAY_NAME;
    }

    public String determineSortDirection(String sortDirection){
        if(sortDirection != null){
            return "desc".equalsIgnoreCase(sortDirection)? "DESC": "ASC";
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
