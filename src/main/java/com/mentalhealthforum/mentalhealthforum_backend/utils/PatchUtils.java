package com.mentalhealthforum.mentalhealthforum_backend.utils;

import org.openapitools.jackson.nullable.JsonNullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility class for handling partial updates.
 *
 * <p><strong>Rule of thumb:</strong> Use this class for
 * {@code DTO Requests handling partial HTTP PATCH payloads (using JsonNullable<T> + PatchUtils)}.
 * It cleanly separates fields that were omitted by the client from fields explicitly set to null.
 */
public final class PatchUtils {

    private PatchUtils(){
        // Prevent instantiation
    }

    /**
     * Strict patch execution for fields that cannot be cleared to null (e.g., First/Last name).
     * Updates ONLY if the field is present in the payload AND the value is not null/blank.
     */
    public static <T> boolean patchStrict(JsonNullable<T> jsonNullable, T currentValue, Consumer<T> setter) {
        if (jsonNullable == null || !jsonNullable.isPresent()) {
            return false;
        }

        T newValue = jsonNullable.get();
        if (newValue == null) {
            return false; // Ignore explicit null attempts for strict fields
        }

        return applyIfChanged(newValue, currentValue, setter);
    }

    /**
     * Permissive String patch supporting explicit null clearing and automatic whitespace trimming.
     */
    public static boolean patchAllowNull(JsonNullable<String> jsonNullable, String currentValue, Consumer<String> setter){
        if (jsonNullable == null || !jsonNullable.isPresent()) {
            return false;
        }

        String newValue = jsonNullable.get();
        if(newValue != null){
            newValue = newValue.trim();
        }

        return applyIfChanged(newValue, currentValue, setter);
    }

    /**
     * Permissive Set patch supporting explicit null clearing and safe collection copying.
     */
    public static boolean patchAllowNull(JsonNullable<Set<String>> jsonNullable, Set<String> currentValue, Consumer<Set<String>> setter){
        if (jsonNullable == null || !jsonNullable.isPresent()) {
            return false;
        }

        Set<String> rawNew = jsonNullable.get();
        Set<String> safeNew = (rawNew == null) ? Set.of() : new HashSet<>(rawNew);
        Set<String> safeCurrent = (currentValue == null) ? Set.of() : currentValue;

        if (!Objects.equals(safeNew, safeCurrent)) {
            setter.accept(safeNew);
            return true;
        }
        return false;
    }

    /**
     * Permissive generic patch for any type (Enums, Numbers, Booleans, Objects)
     * supporting explicit null clearing and change detection
     * */
    public static <T> boolean patchAllowNull(JsonNullable<T> jsonNullable, T currentValue, Consumer<T> setter){
        if(jsonNullable == null || !jsonNullable.isPresent()){
            return false;
        }

        return applyIfChanged(jsonNullable.get(), currentValue, setter);
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
