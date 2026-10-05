

---

# Listing Pattern — Benchmark 

**Status:** Validated against 12 listings (pending invites, user history, users, bookmarks, categories, threads, reports, posts, watch threads, focus categories, tags, connections).

**Purpose:** Define the shape, behavior, and design vocabulary every paginated listing endpoint shares. New listings must conform; existing listings are the reference implementations.

---

## 1. The Listing Skeleton

Every listing method follows this narrative, in order:

```
1. Pagination guard
2. Offset computation
3. Viewer context resolution (if needed)
4. Effective-value normalization (once, before the repo call)
5. Repository call — list + count
6. Empty-result short-circuit
7. Enrichment (batch; no N+1)
8. Filter metadata build
9. PaginatedResponse assembly
```

Mechanics may vary by listing. Responsibility and order are fixed.

**Guard:**
```java
if (page < 0 || size <= 0) {
    log.error("Invalid pagination parameters (<method>): page={}, size={}", page, size);
    throw new InvalidPaginationException();
}
int offset = page * size;
```

**Normalization:** all request inputs become `effective*` locals before the repo call. No normalization inside `flatMap`.

```java
String[] effectiveX = XEnum.toXNames(x);
String   effectiveSearch = blankToNull(search);
XSortField sortByField = (sortBy != null) ? sortBy : XSortField.DEFAULT;
String   effectiveSortDirection = sortByField.determineSortDirection(sortDirection);
```

---

## 2. Response Envelope

Every listing returns:

```java
StandardSuccessResponse<PaginatedResponse<TResponse>>
```

via `ResponseEntity.ok(...)`.

```java
PaginatedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalCount,
    FilterMetadata<XFilterDto> filters
)
```

The `filters` field is always present on the non-empty path. See §6 for zero-filter listings.

---

## 3. `FilterOption`

**Wire shape — flat and uniform.** Every dimension uses the same shape. `NON_NULL` strips absent fields.

```json
{
  "kind": "ENUM" | "USER" | "ENTITY",
  "id": "uuid-or-null",
  "label": "Display Label",
  "value": "string-value",
  "avatarUrl": "url-or-null",
  "initials": "JD-or-null",
  "count": 42
}
```

**Java — three named factories. No public constructors. No `@Builder`.**

```java
FilterOption.ofEnum(label, value, count)
FilterOption.ofUser(id, label, avatarUrl, initials, count)
FilterOption.ofEntity(id, label, value, count)
```

| Factory | `kind` | `id` | `value` | `avatarUrl` / `initials` |
|---|---|---|---|---|
| `ofEnum` | `ENUM` | — | enum name | — |
| `ofUser` | `USER` | ✅ | id-as-string | ✅ |
| `ofEntity` | `ENTITY` | ✅ | domain identifier | — |

**Why flat:** the wire contract is the API. Frontend consumes one TypeScript type across every listing and every dimension. Java-side ergonomics (factories) never leak into the serialized shape.

**Discriminated union:** `kind` labels the semantic category so the frontend can dispatch to the correct rendering component (`UserFilterOption`, `EntityFilterOption`, `EnumFilterOption`) without inferring from field presence.

---

## 4. Filter Metadata ↔ Query Params

**Invariant:** every dropdown-able query parameter has exactly one corresponding `FilterOption` dimension in the listing's `XFilterDto`. No more, no fewer.

**Classification:**

| Param type | Dropdown-able? | Dimension? |
|---|---|---|
| Enum | ✅ | ✅ |
| `UUID` (entity, user) — a *facet* | ✅ | ✅ |
| `UUID` — a *routing/mode param* | ❌ | ❌ |
| `Boolean` (toggle) | ❌ | ❌ |
| `String search` | ❌ | ❌ |
| `sort_by` / `sort_direction` | ❌ | ❌ |

**Facet vs. mode selector:**
- **Facet** — the result set can be meaningfully grouped by it. Multiple rows share the same value. → dimension.
- **Mode selector** — changes which records come back (scope, direction, target). → no dimension.

Example: reports target threads (facet → `threads` dimension). Posts live in threads (facet → `threads` dimension). `is_deleted`, `connection_type`, `post_id` (mode/routing) → no dimension.

**Declared = populated.** Every field in `XFilterDto` is set by `buildXFilters`. No dead fields.

---

## 5. Building a Dimension

**Enum dimension (`ofEnum`)** — iterate `Enum.values()`, attach counts, filter zeros:

```java
Map<EnumType, Long> counts = records.stream()
        .map(r -> EnumType.fromString(r.rawField()))   // if source is raw string
        // OR: .map(EnumType::fromRecord)              // if source is already the enum
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

List<FilterOption> options = Arrays.stream(EnumType.values())
        .map(e -> FilterOption.ofEnum(e.getDisplayName(), e.name(), counts.getOrDefault(e, 0L)))
        .filter(o -> o.getCount() > 0)
        .toList();
```

- **Natural order** — the enum's declaration order. No `.sorted(...)`.
- **Zero-count options dropped.**

**User dimension (`ofUser`):**

```java
List<FilterOption> options = userMap.entrySet().stream()
        .map(e -> {
            long count = counts.getOrDefault(e.getKey(), 0L);
            UserDetails u = e.getValue();
            return FilterOption.ofUser(e.getKey(), u.getDisplayName(), u.getAvatarUrl(), u.getInitials(), count);
        })
        .sorted(Comparator.comparing(FilterOption::getLabel))
        .toList();
```

- **Sorted by label alphabetically.**
- Zero-count filtering not needed — entries only exist if the user is in the result set.

**Entity dimension (`ofEntity`):**

```java
List<FilterOption> options = entityMap.entrySet().stream()
        .map(e -> {
            long count = counts.getOrDefault(e.getKey(), 0L);
            Entity entity = e.getValue();
            return FilterOption.ofEntity(e.getKey(), entity.getName(), entity.getSlug(), count);
        })
        .sorted(Comparator.comparing(FilterOption::getLabel))
        .toList();
```

- **Sorted by label alphabetically.**
- Same zero-count reasoning as `ofUser`.

**Sort rules summary:**

| Factory | Order | Zero-filter |
|---|---|---|
| `ofEnum` | enum declaration order | ✅ dropped |
| `ofUser` | label alphabetical | — |
| `ofEntity` | label alphabetical | — (parent categories: ✅ dropped) |

---

## 6. Zero-Filter Listings

If a listing has **no dropdown-able params**, it has no `XFilterDto` and no `buildXFilters`. `FilterMetadata` carries only `sortOptions`:

```java
FilterMetadata<Object> filters = FilterMetadata.builder()
        .sortOptions(getXSortOptions())
        .build();

return new PaginatedResponse<>(content, page, size, total, filters);
```

Examples: focus categories, category tags, connections.

The `filters` field is still present. `NON_NULL` strips the absent dimension fields, leaving only `sortOptions`.

---

## 7. Sort Enum Contract

Every listing has an `XSortField` enum implementing **all six members**:

```java
@Getter
public enum XSortField {
    VALUE_A("db_column_a", "label a", "ASC"),
    VALUE_B("db_column_b", "label b", "DESC");

    private final String value;            // exact string the repository uses
    private final String label;            // human-readable label
    private final String defaultDirection; // "ASC" | "DESC"

    public static final XSortField DEFAULT = XSortField.VALUE_A;

    public static XSortField fromString(String value) { ... }
    public String determineSortDirection(String sortDirection) { ... }
    public SortOption toSortOption() { ... }
}
```

**Rules:**
- `value` = exact string the repository query consumes. No aliasing.
- `label` = human-readable.
- `defaultDirection` — per-field, not hardcoded.
- `DEFAULT` = the fallback when `sortBy` is null.
- `toSortOption()` sets `.value(this.name())` (uppercase, matches Spring binding), `.label(this.label)`, `.defaultDirection(this.defaultDirection)`, `.isDefault(this == DEFAULT)`.

**Wire value is the enum constant name** — uppercase, snake_case words. Matches how Spring binds `sort_by`. `fromString` remains case-insensitive for internal use, but the wire contract is uppercase.

**Enum params on the wire are case-sensitive.** Lowercase or mixed-case returns 400 — no silent fallback.

**Enum rank = declaration order.** When a sort field ranks an enum, the SQL rank must match the enum's declaration order. ASC = declaration order, DESC = reverse. This keeps "ascending" semantically consistent across all sort fields.

---

## 8. Controller Conventions

```java
@GetMapping
public Mono<ResponseEntity<StandardSuccessResponse<PaginatedResponse<TResponse>>>> getX(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false, name = "snake_case_param") Type param,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(defaultValue = "DEFAULT_ENUM", name = "sort_by") XSortField sortBy,
        @RequestParam(required = false, name = "sort_direction") String sortDirection
){
    ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
    return xService.getX(page, size, param, search, sortBy, sortDirection, viewerContext)
            .map(payload -> ResponseEntity.ok(
                    new StandardSuccessResponse<>("<message>.", payload)));
}
```

**Rules:**
- **snake_case in `name=`, camelCase in Java.**
- **Defaults on annotations only.** Service re-guards.
- **Typed `sortBy`** — the enum, not `String`. No `validateAndNormalizeSortBy` helper.
- **Typed filter params** — enums bound directly, not raw strings.
- **`search` is `required = false`** — not `defaultValue = ""`.
- **No `@Parameter` annotations** on listing endpoints.
- **One `ViewerContext` extraction per request.**
- **Thin** — no logic beyond extraction and wrapping.

---

## 9. Invariants

Rules that hold across every listing:

1. **Metadata mirrors dropdown-able params.** Count matches; no dead dimensions.
2. **Declared = populated.** Every `XFilterDto` field is set by its builder.
3. **Enum dimensions use `Arrays.stream(values())`,** not `entrySet().stream()`.
4. **Enum dimensions drop zero-count options.**
5. **Facets get dimensions; mode selectors don't.**
6. **Sort enum `value` = DB column; `toSortOption().value` = enum name.**
7. **`DEFAULT` on every sort enum; `isDefault` on exactly one `SortOption`.**
8. **`FilterOption` construction always goes through a factory.**
9. **Enum params on the wire are uppercase and case-sensitive.**

---

## 10. Not Standardized (by design)

These vary per listing and are not part of the pattern:

- Enrichment carrier shape (`EnrichedXData`) and its fields.
- Zip timing (list + count before or after enrichment).
- Multi-endpoint variants (admin/my, all/active) and whether they share a private executor.
- Anonymization / context-based mapping logic.
- Response DTO style (record, `@Builder` class, `@Getter/@Setter` with mapper).
- Message strings on `StandardSuccessResponse`.
- Whether `toSortOption` is public or private (as long as the enum owns its option list).
- Label casing on sort fields.
- Whether `@AuthenticationPrincipal Jwt` is present (public endpoints omit it).

If any of these start showing up as a repeated pain point across listings, promote them into the pattern.

---

## 11. Open Items

Deferred — noted for future consideration, not currently part of the pattern:

**11.1 Empty-page response parity.**
Empty pages currently return `PaginatedResponse<>(List.of(), page, size, 0L)` — no `filters`, and `total` hardcoded to `0L`. Non-empty pages carry `FilterMetadata`. Consider making empty and non-empty structurally identical.

**11.2 Enum descriptions.**
Add a `description` field to facet enums alongside `displayName`. When added, expose it on `FilterOption` (wire-visible change). Deferred until after the current sweep ships.

**11.3 Enum↔DB coupling test.**
Enums carry DB-bound strings (`SortField.value`, `ConnectionType.name()` used in SQL). Nothing currently catches a rename that breaks the coupling. Consider an ArchUnit-style test.

**11.4 "Declared = populated" test.**
A test that asserts every `XFilterDto` field is populated by `buildXFilters`.

**11.5 Additional facets on zero-filter listings.**
Listings 10 (`focusCategories`), 11 (`tags`), 12 (`connections`) could gain facet dimensions (e.g., `parentCategories` on focus, `createdBy` on tags, `status` on connections). Slots cleanly into the existing pattern when added.

**11.6 `sort_order` as a positional attribute.**
Categories' `sort_order` is a positional attribute, not a view field. Consider a separate `PATCH /categories/reorder` endpoint rather than a `sort_by=SORT_ORDER` listing.

**11.7 `current_user_first` default on users listing.**
Currently defaults to `true`, which pins the current user at row 1 regardless of sort. Consider defaulting to `false` for predictability.

---



