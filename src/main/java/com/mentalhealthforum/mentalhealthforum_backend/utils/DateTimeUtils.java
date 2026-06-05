package com.mentalhealthforum.mentalhealthforum_backend.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class DateTimeUtils {

    private DateTimeUtils(){
        // Prevent instantiation
    }

    private static final DateTimeFormatter HUMAN_READABLE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("MMMM d, yyyy 'at' h:mm a z")
                    .toFormatter(Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());


    /**
     * Formats an Instant to a human-readable string.
     * Example: "July 3, 2026 at 10:00 AM EAT"
     * */
    public static String toHumanReadable(Instant instant, String defaultValue){
        if(instant == null){
            return defaultValue;
        }
        return HUMAN_READABLE_FORMAT.format(instant);
    }
}
