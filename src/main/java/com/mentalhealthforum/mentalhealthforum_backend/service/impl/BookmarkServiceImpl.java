package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkRequest;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.BookmarkedThreadRecord;
import com.mentalhealthforum.mentalhealthforum_backend.dto.forumCategoriesHierarchicalAndTagged.CategoryDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.BookmarkFilterDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ContentWarningType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ThreadType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.BookmarkSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.CategoryEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.BookmarkEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.CategoryRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.ThreadRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.ThreadBookmarkRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.AppUserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.BookmarkService;
import com.mentalhealthforum.mentalhealthforum_backend.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;

import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    private final TransactionalOperator transactionalOperator;
    private final ThreadBookmarkRepository bookmarkRepository;
    private final ThreadRepository threadRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    public BookmarkServiceImpl(
            TransactionalOperator transactionalOperator,
            ThreadBookmarkRepository bookmarkRepository,
            ThreadRepository threadRepository,
            CategoryRepository categoryRepository,
            CategoryService categoryService,
            AppUserRepository appUserRepository,
            AppUserService appUserService) {
        this.transactionalOperator = transactionalOperator;
        this.bookmarkRepository = bookmarkRepository;
        this.threadRepository = threadRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
    }

    @Override
    public Mono<BookmarkResponse> addBookmark(BookmarkRequest request, ViewerContext viewerContext){

        UUID userId = UUID.fromString(viewerContext.getUserId());
        UUID threadId = request.threadId();

        return validateThreadVisible(threadId, viewerContext)
                .then(checkNotAlreadyBookmarked(userId, threadId))
                .then(createBookmark(userId, threadId, request.notes()))
                .flatMap(this::enrichSingleBookmarkWithData)
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<Void> removeBookmark(UUID threadId, ViewerContext viewerContext){
        UUID userId = UUID.fromString(viewerContext.getUserId());

        return bookmarkRepository.deleteByUserIdAndThreadId(userId, threadId)
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<PaginatedResponse<BookmarkResponse>> getMyBookmarks(
            int page,
            int size,
            UUID categoryId,
            UUID creatorId,
            ThreadType threadType,
            ThreadStatus threadStatus,
            Boolean hasContentWarning,
            String search,
            BookmarkSortField sortBy,
            String sortDirection,
            ViewerContext viewerContext
    ){

        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters: page={}, size={}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();


        String effectiveSearch = (search == null || search.isBlank()) ? null : search.trim();
        String effectiveThreadType =  threadType != null? threadType.name() : null;
        String effectiveThreadStatus  = threadStatus != null? threadStatus.name() : null;

        BookmarkSortField sortByField = sortBy != null? sortBy: BookmarkSortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);


        return bookmarkRepository.findBookmarkedThreadsPaginated(
                        viewerId,
                        isAdmin, isModeratorOrAdmin, isVerified,
                        categoryId, creatorId,
                        effectiveThreadType,
                        effectiveThreadStatus, hasContentWarning,
                        effectiveSearch, sortByField.getValue(), effectiveSortDirection, size, offset)
                .collectList()
                .zipWith(bookmarkRepository.countBookmarksWithFilters(
                        viewerId,
                        isAdmin, isModeratorOrAdmin, isVerified,
                        categoryId, creatorId,
                        effectiveThreadType,
                        effectiveThreadStatus, hasContentWarning, effectiveSearch))
                .flatMap(tuple -> {
                    List<BookmarkedThreadRecord> records = tuple.getT1();
                    long totalCount = tuple.getT2();

                    if(records.isEmpty()){
                        FilterMetadata<Object> filters = FilterMetadata.builder()
                                .sortOptions(getBookmarkSortOptions())
                                .build();
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, totalCount, filters));
                    }

                    return enrichBookmarksWithBatchData(records)
                            .map(enriched -> {

                                FilterMetadata<BookmarkFilterDto> filters = buildBookmarkFilters(enriched);

                                return new PaginatedResponse<>(enriched.responses, page, size, totalCount, filters);
                            });
                });

    }

    @Override
    public Mono<Boolean> isBookmarked(UUID threadId, ViewerContext viewerContext){
        UUID userId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();
        return bookmarkRepository.existsVisibleByUserIdAndThreadId(userId, threadId, isAdmin, isModeratorOrAdmin, isVerified);
    }

    @Override
    public Mono<Long> getBookmarkCountByUserId(ViewerContext viewerContext){
        UUID userId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();
        return bookmarkRepository.countVisibleByUserId(userId, isAdmin, isModeratorOrAdmin, isVerified);
    }

    @Override
    public Mono<Long> getBookmarkCountForThread(UUID threadId, ViewerContext viewerContext){
        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();
        return bookmarkRepository.countVisibleByThreadId(threadId, viewerId, isAdmin, isModeratorOrAdmin, isVerified);
    }


    // ==================== PRIVATE HELPERS ====================

    private Mono<Void> validateThreadVisible(UUID threadId, ViewerContext viewerContext) {
        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();

        return threadRepository.findByIdAndIsDeletedFalse(threadId)
                .switchIfEmpty(Mono.error(new ApiException("Thread not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(thread ->
                        categoryRepository.isCategoryVisible(thread.getCategoryId(), viewerId, isAdmin, isModeratorOrAdmin, isVerified)
                                .flatMap(visible -> {
                                    if(!visible){
                                        return Mono.error(new ApiException("You do not have permission to access this thread", ErrorCode.FORBIDDEN));
                                    }
                                    return Mono.empty();
                                })
                )
                .then();
    }

    private Mono<Void> checkNotAlreadyBookmarked(UUID userId, UUID threadId) {
        return bookmarkRepository.existsByUserIdAndThreadId(userId, threadId)
                .flatMap(exists -> {
                    if(exists){
                        return Mono.error(new ApiException("Thread already bookmarked", ErrorCode.VALIDATION_FAILED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<BookmarkedThreadRecord> createBookmark(UUID userId, UUID threadId, String notes){
        BookmarkEntity bookmarkEntity = BookmarkEntity.builder()
                .userId(userId)
                .threadId(threadId)
                .notes(notes)
                .build();

        return bookmarkRepository.save(bookmarkEntity)
                .flatMap(bookmark -> bookmarkRepository.findBookmarkById(bookmark.getId(), bookmark.getUserId()));
    }

    private BookmarkSortField validateAndNormalizeSortBy(String sortBy) {
       return BookmarkSortField.fromString(sortBy);
    }


    /**
     * Enriches a single bookmark..
     */
    private Mono<BookmarkResponse> enrichSingleBookmarkWithData(BookmarkedThreadRecord record) {

        return Mono.zip(appUserService.getUserDetails(record.creator_id()), categoryService.getCategoryDetails(record.category_id()))
                .map(tuple -> {
                     UserDetails creator = tuple.getT1();
                     CategoryDetails categoryDetails = tuple.getT2();

                     return mapResponseWithData(record, creator, categoryDetails);
                });
    }
    /**
     * Enriches a list of bookmarked thread records with creator details using batch fetching.
     * Uses batch fetching to avoid N+1 queries.
     */
    private Mono<EnrichedBookmarkData> enrichBookmarksWithBatchData(
        List<BookmarkedThreadRecord> records
    ){
        if(records.isEmpty()){
            return Mono.just(new EnrichedBookmarkData(
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of()
            ));
        }

        // Extract unique creator IDs
        List<UUID> creatorIds = records.stream()
                .map(BookmarkedThreadRecord::creator_id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Extract unique category IDs
        List<UUID> categoryIds = records.stream()
                .map(BookmarkedThreadRecord::category_id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        // Batch fetch all creators
        Mono<Map<UUID, UserDetails>> creatorsMap = appUserRepository
                .findAppUsersByKeycloakIds(creatorIds)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());

        // Batch fetch all categories
        Mono<Map<UUID, CategoryDetails>> categoriesMap = categoryRepository
                .findCategoriesByIds(categoryIds)
                .collectMap(CategoryEntity::getId, CategoryEntity::toCategoryDetails)
                .defaultIfEmpty(new HashMap<>());

        return Mono.zip(
                creatorsMap,
                categoriesMap
        ).map(tuple -> {
            Map<UUID, UserDetails> creators = tuple.getT1();
            Map<UUID, CategoryDetails> categories = tuple.getT2();


            List<BookmarkResponse> responses = records.stream()
                    .map(record -> {
                        UserDetails creatorDetails = creators.getOrDefault(record.creator_id(), AppUserEntity.defaultUser());
                        CategoryDetails categoryDetails = categories.getOrDefault(record.category_id(), CategoryEntity.defaultCategory());

                        return mapResponseWithData(record, creatorDetails, categoryDetails);
                    })
                    .toList();

            return new EnrichedBookmarkData(
                    responses,
                    records,
                    creators,
                    categories
            );
        });

    }

    private BookmarkResponse mapResponseWithData(
        BookmarkedThreadRecord record,
        UserDetails threadCreator,
        CategoryDetails category
    ){
        return BookmarkResponse.builder()
                // Bookmark metadata
                .id(record.bookmark_id())
                .notes(record.bookmark_notes())
                .bookmarkedAt(record.bookmarked_at())
                // References (nested)
                .thread(record.toThreadDetails())
                .threadCreator(threadCreator)
                .category(category)
                .build();
    }

    private record EnrichedBookmarkData(
            List<BookmarkResponse> responses,
            List<BookmarkedThreadRecord> records,
            Map<UUID, UserDetails> creators,
            Map<UUID, CategoryDetails> categories
    ) {}

    private FilterMetadata<BookmarkFilterDto> buildBookmarkFilters(EnrichedBookmarkData data){
        // Build creator options
        Map<UUID, Long> creatorCounts = data.records.stream()
                .collect(Collectors.groupingBy(
                        BookmarkedThreadRecord::creator_id,
                        Collectors.counting()
                ));

        List<FilterOption> creatorOptions = data.creators().entrySet().stream()
                .map(entry -> {
                    UUID creatorId = entry.getKey();
                    UserDetails creator = entry.getValue();
                    long count = creatorCounts.getOrDefault(creatorId, 0L);
                    return  FilterOption.ofUser(
                            creatorId,
                            creator.getDisplayName(),
                            creator.getAvatarUrl(),
                            creator.getInitials(),
                            count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build category options
        Map<UUID, Long> categoryCounts = data.records.stream()
                .collect(Collectors.groupingBy(
                        BookmarkedThreadRecord::category_id,
                        Collectors.counting()
                ));

        List<FilterOption> categoryOptions = data.categories.entrySet().stream()
                .map(entry -> {
                    UUID categoryId = entry.getKey();
                    CategoryDetails category = entry.getValue();
                    long count = categoryCounts.getOrDefault(categoryId, 0L);
                    return FilterOption.ofEntity(
                            categoryId,
                            category.getName(),
                            category.getSlug(),
                            count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build thread type options
        Map<ThreadType, Long> threadTypeCounts = data.records.stream()
                .map(record -> ThreadType.fromString(record.thread_type()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        List<FilterOption> threadTypeOptions = Arrays.stream(ThreadType.values())
                .map(threadType -> FilterOption.ofEnum(
                        threadType.getDisplayName(),
                        threadType.name(),
                        threadTypeCounts.getOrDefault(threadType, 0L)
                ))
                .filter(option -> option.getCount() > 0)
                .toList();


        // Build thread status options
        Map<ThreadStatus, Long> threadStatusCounts = data.records.stream()
                .map(record -> ThreadStatus.fromString(record.thread_status()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        List<FilterOption> threadStatusOptions = Arrays.stream(ThreadStatus.values())
                .map(threadStatus -> FilterOption.ofEnum(
                        threadStatus.getDisplayName(),
                        threadStatus.name(),
                        threadStatusCounts.getOrDefault(threadStatus, 0L)
                ))
                .filter(option -> option.getCount() > 0)
                .toList();

        BookmarkFilterDto bookmarkFilters = BookmarkFilterDto.builder()
                .creators(creatorOptions)
                .categories(categoryOptions)
                .threadTypes(threadTypeOptions)
                .threadStatuses(threadStatusOptions)
                .build();

        return FilterMetadata.<BookmarkFilterDto>builder()
                .filters(bookmarkFilters)
                .sortOptions(getBookmarkSortOptions())
                .build();

    }

    private List<SortOption> getBookmarkSortOptions() {
        return Arrays.stream(
                BookmarkSortField.values())
                .map(BookmarkSortField::toSortOption)
                .toList();
    }

}
