package com.mentalhealthforum.mentalhealthforum_backend.enums.listings;

import lombok.Getter;

@Getter
public enum ReportSortField {

    SEVERITY("severity"),
    REPORTED_AT("reported_at"),
    LAST_MODIFIED_AT("last_modified_at");


    private final String value;

    ReportSortField(String value) {
        this.value = value;
    }

    public static ReportSortField fromString(String value) {
        if(value == null){
            return REPORTED_AT;  // Default to most recent
        }
        for(ReportSortField field : ReportSortField.values()){
            if(field.getValue().equalsIgnoreCase(value)){
                return field;
            }
        }
        return  REPORTED_AT;
    }

}
