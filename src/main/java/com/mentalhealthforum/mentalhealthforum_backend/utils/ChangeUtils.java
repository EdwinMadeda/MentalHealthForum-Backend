package com.mentalhealthforum.mentalhealthforum_backend.utils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility class for safely updating raw fields only if their values have changed.
 *
 * <p><strong>Rule of thumb:</strong> Use this class for
 * {@code Internal database syncs, full entity loads, or Keycloak DTO mappings where data is already unboxed/raw (using ChangeUtils)}.
 */
public final class ChangeUtils {

    private ChangeUtils() {
        // Prevent instantiation
    }

    /**
     * Strict version that ignores null values entirely (commonly used for Keycloak mappings).
     */
    public static <T> boolean setIfChangedStrict(T newValue, T currentValue, Consumer<T> setter) {
        if (newValue == null) return false; // ignore null
        return applyIfChanged(newValue, currentValue, setter);
    }

    /**
     * String version with automatic trimming and blank-checking rules.
     */
    public static boolean setIfChanged(String newValue, String currentValue, Consumer<String> setter) {
        if (newValue != null) newValue = newValue.trim();
        if(newValue != null && newValue.isBlank()) return false;
        return applyIfChanged(newValue, currentValue, setter);

    }

    /**
     * Set version ensuring safe HashSet instantiation and deep collection comparison.
     */
    public static boolean setIfChanged(Set<String> newValue, Set<String> currentValue, Consumer<Set<String>> setter) {
        Set<String> safeNew = (newValue == null) ? Set.of() : new HashSet<>(newValue);
        Set<String> safeCurrent = (currentValue == null) ? Set.of() : currentValue;

        if (!safeNew.equals(safeCurrent)) {
            setter.accept(safeNew);
            return true;
        }
        return false;
    }

    /**
     * Generic version for any type (enums, numbers, etc.)
     * Only updates if values are different (null-safe)
     */
    public static <T> boolean setIfChanged(T newValue, T currentValue, Consumer<T> setter){
        return applyIfChanged(newValue, currentValue, setter);
    }

    /**
     * Central private evaluation engine to keep execution completely DRY.
     */
    private static <T> boolean applyIfChanged(T newValue, T currentValue, Consumer<T> setter) {
        if(!Objects.equals(newValue, currentValue)){
            setter.accept(newValue);
            return true;
        }
        return false;
    }


}
