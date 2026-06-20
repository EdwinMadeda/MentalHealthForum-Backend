package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import lombok.Getter;

@Getter
public enum ConnectionSortField {
    CREATED_AT("created_at"),
    DISPLAY_NAME("display_name");

    private final String value;

    ConnectionSortField(String value) {
        this.value = value;
    }

    public static ConnectionSortField fromString(String value) {
        if(value == null){
            return CREATED_AT;  // Default to created_at
        }
        for(ConnectionSortField field : ConnectionSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  CREATED_AT;
    }

}
