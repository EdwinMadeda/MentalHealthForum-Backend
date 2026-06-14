package com.mentalhealthforum.mentalhealthforum_backend.enums;

public enum ContentWarningType {
      NONE,
      SELF_HARM,
      SUICIDE,
      TRAUMA,
      ABUSE,
      VIOLENCE,
      SUBSTANCE_USE,
      EATING_DISORDERS;

      public static ContentWarningType fromString(String value){
            if(value == null){
                  return null;
            }
            try {
                  return ContentWarningType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                  return null;
            }
      }
}
