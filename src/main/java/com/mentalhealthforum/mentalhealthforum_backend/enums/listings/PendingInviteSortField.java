package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import lombok.Getter;

@Getter
public enum PendingInviteSortField {
    USERNAME("username","username" , "ASC"),
    EMAIL("email", "email","ASC" ),
    DATE_CREATED("date_created","date created" , "DESC");

    private final String value;
    private final String label;
    private final String defaultDirection;

    PendingInviteSortField(String value, String label, String defaultDirection) {
        this.value = value;
        this.label = label;
        this.defaultDirection = defaultDirection;
    }

    public static PendingInviteSortField fromString(String value) {
        if(value == null){
            return DATE_CREATED;  // Default to date created
        }
        for(PendingInviteSortField field : PendingInviteSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  DATE_CREATED;
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
