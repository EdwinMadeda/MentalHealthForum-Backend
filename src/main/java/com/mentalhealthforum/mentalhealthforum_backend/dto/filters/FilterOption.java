package com.mentalhealthforum.mentalhealthforum_backend.dto.filters;

import com.mentalhealthforum.mentalhealthforum_backend.enums.FilterOptionKind;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class FilterOption {
    private UUID id;
    private String label;
    private String value;
    private String avatarUrl;
    private String initials;
    private Long count;
    private FilterOptionKind kind;

    private FilterOption(UUID id, String label, String value, String avatarUrl, String initials, Long count, FilterOptionKind kind){
        this.id = id;
        this.label = label;
        this.value = value;
        this.avatarUrl = avatarUrl;
        this.initials = initials;
        this.count = count;
        this.kind = kind;
    }

    /**
     * For enum-backed filter dimensions (stages, roles, groups, actions, etc).
     * kind = ENUM, no id / avatar / initials — stripped by NON_NULL.
     */
    public static FilterOption ofEnum(String label, String value, Long count){
        return new FilterOption(null, label, value, null, null, count, FilterOptionKind.ENUM);
    }

    /**
     * For user-backed filter dimensions (inviters, performedBy's, target users, etc.).
     * kind = USER, value derived from id so the wire shape stays uniform.
     */
    public static FilterOption ofUser(UUID id, String label, String avatarUrl, String initials, Long count){
        return new FilterOption(id, label, id != null? id.toString(): null, avatarUrl, initials , count, FilterOptionKind.USER);
    }

    /**
     * For entity-backed filter dimensions that carry an id but are not users
     * (categories, tags, threads, etc.).
     * kind = ENTITY, value passed in explicitly (may equal id.toString() or be a distinct identifier).
     */
    public static FilterOption ofEntity(UUID id, String label, String value, Long count){
        return new FilterOption(id, label, value, null, null, count, FilterOptionKind.ENTITY);
    }

}
