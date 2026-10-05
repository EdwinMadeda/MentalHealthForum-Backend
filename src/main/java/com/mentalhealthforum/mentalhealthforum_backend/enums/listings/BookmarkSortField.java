package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum BookmarkSortField {
    TITLE("title", "title", "ASC"),
    BOOKMARKED_AT("bookmarked_at", "bookmarked at", "DESC"),
    LAST_ACTIVITY_AT("last_activity_at", "last activity at", "DESC"),
    POST_COUNT("post_count", "post count", "DESC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    BookmarkSortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static final BookmarkSortField DEFAULT = BookmarkSortField.BOOKMARKED_AT;

    public static BookmarkSortField fromString(String value) {
        if(value == null){
            return DEFAULT;  // Default to bookmarked_at
        }
        for(BookmarkSortField field : BookmarkSortField.values()){
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
