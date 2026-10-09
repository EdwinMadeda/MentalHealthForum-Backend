package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.threadLifecycleAndMetadata.ThreadDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.PostFilterDto;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.postsRicherContentAndSafety.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.PostSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.ThreadEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.PostEditHistoryEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.PostEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.*;
import com.mentalhealthforum.mentalhealthforum_backend.service.AnonymousNameGenerator;
import com.mentalhealthforum.mentalhealthforum_backend.service.PostService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final TransactionalOperator transactionalOperator;
    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final PostEditHistoryRepository postEditHistoryRepository;
    private final AnonymousNameGenerator anonymousNameGenerator;
    private final UserModerationService userModerationService;

    public PostServiceImpl(
            TransactionalOperator transactionalOperator,
            PostRepository postRepository,
            ThreadRepository threadRepository,
            CategoryRepository categoryRepository,
            AppUserRepository appUserRepository,
            PostEditHistoryRepository postEditHistoryRepository,
            AnonymousNameGenerator anonymousNameGenerator,
            UserModerationService userModerationService) {
        this.transactionalOperator = transactionalOperator;
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
        this.categoryRepository = categoryRepository;
        this.appUserRepository = appUserRepository;
        this.postEditHistoryRepository = postEditHistoryRepository;
        this.anonymousNameGenerator = anonymousNameGenerator;
        this.userModerationService = userModerationService;
    }

    // ==================== USER ACTIONS ====================

    @Override
    public Mono<PostResponse> createPost(CreatePostRequest request, ViewerContext viewerContext) {
        String userId = viewerContext.getUserId();

        return appUserRepository.findAppUserByKeycloakId(userId)
                .switchIfEmpty(Mono.error(new ApiException("User not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(appUser -> userModerationService.requireNotMuted(appUser.getKeycloakId(), "create posts")
                        .then(findActiveThread(request.getThreadId(), viewerContext))
                )
                .flatMap(thread -> validateParentPost(thread, request.getParentPostId()))
                .flatMap(thread -> createAndSavePost(request, userId, thread.getId()))
                .flatMap(this::enrichSinglePostWithData)
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<PostResponse> getPost(UUID postId, ViewerContext viewerContext) {
        return findPostWithVisibility(postId, viewerContext)
                .flatMap(this::enrichSinglePostWithData);
    }

    @Override
    public Mono<PaginatedResponse<PostResponse>> getAllPosts(
            int page,
            int size,
            UUID threadId,
            UUID authorId,
            UUID parentPostId,
            PostType postType,
            Boolean hasContentWarning,
            Boolean isDeleted,
            String search,
            PostSortField sortBy,
            String sortDirection,
            ViewerContext viewerContext
    ) {

        UUID currentUserId = UUID.fromString(viewerContext.getUserId());

        // Handle deleted posts visibility
        if (isDeleted != null && isDeleted) {
            boolean canViewAllDeleted = ModerationAction.VIEW_DELETED_POSTS.isAllowedFor(viewerContext);

            if(!canViewAllDeleted){
                // Regular users can only see their own deleted posts
                authorId = currentUserId;
            }
        }

        return executeGetPostsQuery(page, size, threadId, authorId, parentPostId, postType, hasContentWarning, isDeleted,
                search, sortBy, sortDirection, viewerContext);
    }

    @Override
    public Mono<PostResponse> updateOwnPost(UUID postId, UpdatePostRequest request, ViewerContext viewerContext) {
        return performUserAction(
                postId,
                viewerContext,
                "update post",
                post -> {
                    // Capture previous state including content warnings
                    String previousContent = post.getContent();
                    Integer previousWordCount = post.getWordCount();
                    ContentWarningType previousContentWarningType = post.getContentWarningType();
                    String previousContentWarningCustomText = post.getContentWarningCustomText();

                    // Apply updates
                    post.setContent(request.getContent());
                    post.setContentWarningType(request.getContentWarningType());
                    post.setContentWarningCustomText(request.getContentWarningCustomText());
                    post.setIsEdited(true);
                    post.setEditReasonType(request.getEditReason());
                    post.setEditedByUserId(UUID.fromString(viewerContext.getUserId()));
                    post.setUpdatedAt(Instant.now());

                    String customEditReason = request.getEditReason() == EditReason.OTHER ? request.getEditReasonCustomText() : null;
                    post.setEditReasonCustomText(customEditReason);

                    // Save history with previous warning state
                    PostEditHistoryEntity history = PostEditHistoryEntity.builder()
                            .postId(post.getId())
                            .previousContent(previousContent)
                            .previousWordCount(previousWordCount)
                            .previousContentWarningType(previousContentWarningType)
                            .previousContentWarningCustomText(previousContentWarningCustomText)
                            .editedBy(UUID.fromString(viewerContext.getUserId()))
                            .editReasonType(request.getEditReason())
                            .editReasonCustomText(customEditReason)
                            .isModeratorEdit(false)
                            .build();

                    return postEditHistoryRepository.save(history)
                            .then(postRepository.save(post))
                            .flatMap(this::enrichSinglePostWithData);
                },
                null,
                null
        );
    }

    @Override
    public Mono<Void> softDeleteOwnPost(UUID postId, ViewerContext viewerContext) {
        return performUserAction(
                postId,
                viewerContext,
                "soft delete post",
                post -> postRepository.softDeletePost(postId),
                null,
                null
        );
    }

    // ==================== MODERATOR ACTIONS ====================

    @Override
    public Mono<Void> softDeleteAnyPost(UUID postId, ViewerContext viewerContext) {
        return ModerationAction.POST_DELETED.checkPermission(viewerContext)
                .then(performModeratorAction(postId, viewerContext,
                        post -> postRepository.softDeletePost(postId),
                        List.of(new ValidationRule(PostEntity::getIsDeleted, "Cannot delete an already deleted post")),
                        true));
    }

    @Override
    public Mono<Void> restorePost(UUID postId, ViewerContext viewerContext) {
        return ModerationAction.POST_RESTORED.checkPermission(viewerContext)
                .then(performModeratorAction(postId, viewerContext,
                        post -> postRepository.restorePost(postId),
                        List.of(new ValidationRule(post -> !post.getIsDeleted(), "Cannot restore a post that is not deleted")),
                        false));
    }

    // ==================== ADMIN ACTIONS ====================

    @Override
    public Mono<PostResponse> addContentWarning(UUID postId, AddContentWarningRequest request, ViewerContext viewerContext){
        UUID moderatorId = UUID.fromString(viewerContext.getUserId());

        return ModerationAction.POST_CONTENT_WARNING_ADDED.checkPermission(viewerContext)
                .then(performModeratorAction(postId, viewerContext,
                        post -> {
                            // Capture previous warning state
                            String previousContent = post.getContent();
                            Integer previousWordCount = post.getWordCount();
                            ContentWarningType previousContentWarningType = post.getContentWarningType();
                            String previousContentWarningCustomText = post.getContentWarningCustomText();

                            // Apply new warning
                            post.setContentWarningType(request.contentWarningType());
                            post.setContentWarningCustomText(request.contentWarningCustomText());
                            post.setIsEdited(true);
                            post.setEditedByUserId(moderatorId);
                            post.setUpdatedAt(Instant.now());

                            // Save history with previous warning state
                            PostEditHistoryEntity history = PostEditHistoryEntity.builder()
                                    .postId(post.getId())
                                    .previousContent(previousContent)
                                    .previousWordCount(previousWordCount)
                                    .previousContentWarningType(previousContentWarningType)
                                    .previousContentWarningCustomText(previousContentWarningCustomText)
                                    .editedBy(moderatorId)
                                    .editReasonType(EditReason.CONTENT_WARNING_ADDED)
                                    .isModeratorEdit(true)
                                    .build();

                            return postEditHistoryRepository.save(history)
                                    .then(postRepository.save(post))
                                    .flatMap(this::enrichSinglePostWithData);
                        },
                        List.of(
                                new ValidationRule(
                                post -> post.getContentWarningType() == request.contentWarningType() &&
                                        Objects.equals(post.getContentWarningCustomText(), request.contentWarningCustomText()),
                                        "Post already has this content warning",
                                        ErrorCode.VALIDATION_FAILED
                                )
                        ),
                        true
                        ));



    }

    @Override
    public Mono<Void> permanentlyDeletePost(UUID postId, ViewerContext viewerContext) {
        return ModerationAction.POST_PERMANENTLY_DELETED.checkPermission(viewerContext)
                .then(findPostWithVisibility(postId, viewerContext))
                .flatMap(postRepository::delete)
                .as(transactionalOperator::transactional);
    }

    // ==================== PRIVATE HELPERS ====================


    private Mono<PostEntity> findPostWithVisibility(UUID postId, ViewerContext viewerContext) {
        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new ApiException("Post not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(post -> validateCategoryVisibleForThread(post.getThreadId(), viewerContext).thenReturn(post));

    }

    private Mono<PostEntity> findActivePost(UUID postId, ViewerContext viewerContext) {
        return findPostWithVisibility(postId, viewerContext)
                .flatMap(post -> {
                    if (post.getIsDeleted()) {
                        return Mono.error(new ApiException("Cannot modify a deleted post", ErrorCode.VALIDATION_FAILED));
                    }
                    return Mono.just(post);
                });
    }

    private Mono<ThreadEntity> findActiveThread(UUID threadId, ViewerContext viewerContext) {
        return validateCategoryVisibleForThread(threadId, viewerContext)
                .flatMap(thread -> {
                    if (thread.getIsDeleted()) {
                        return Mono.error(new ApiException("Cannot post in a deleted thread", ErrorCode.VALIDATION_FAILED));
                    }
                    if (thread.getThreadStatus() == ThreadStatus.CLOSED) {
                        return Mono.error(new ApiException("Cannot post in a closed thread", ErrorCode.VALIDATION_FAILED));
                    }

                    return Mono.just(thread);
                });
    }

    private Mono<ThreadEntity> validateCategoryVisibleForThread(UUID threadId, ViewerContext viewerContext){
        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();

        return threadRepository.findById(threadId)
                .switchIfEmpty(Mono.error(new ApiException("Thread not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .flatMap(thread ->
                        categoryRepository.isCategoryVisible(thread.getCategoryId(), viewerId, isAdmin, isModeratorOrAdmin, isVerified)
                                .flatMap(visible -> {
                                    if(!visible){
                                        return Mono.error(new ApiException("You do not have permission to access this thread", ErrorCode.FORBIDDEN));
                                    }
                                    return Mono.just(thread);
                                })
                );
    }

    private Mono<ThreadEntity> validateParentPost(ThreadEntity thread, UUID parentPostId) {
        if (parentPostId == null) {
            return Mono.just(thread);
        }
        return postRepository.existsByThreadIdAndId(thread.getId(), parentPostId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException("Parent post does not belong to this thread", ErrorCode.VALIDATION_FAILED));
                    }
                    return Mono.just(thread);
                });
    }

    private Mono<PostEntity> createAndSavePost(CreatePostRequest request, String userId, UUID threadId) {
        String anonymousIdentifier = null;
        if (request.getIsAnonymous()) {
            if (request.getCustomAnonymousName() != null && !request.getCustomAnonymousName().isBlank()) {
                anonymousIdentifier = anonymousNameGenerator.normalizeCustomAnonymousName(request.getCustomAnonymousName());
            } else {
                anonymousIdentifier = anonymousNameGenerator.generateAnonymousName(userId, threadId.toString());
            }
        }

        PostEntity post = PostEntity.builder()
                .threadId(request.getThreadId())
                .authorId(UUID.fromString(userId))
                .postType(request.getPostType())
                .parentPostId(request.getParentPostId())
                .content(request.getContent())
                .contentWarningType(request.getContentWarningType())
                .contentWarningCustomText(request.getContentWarningCustomText())
                .isAnonymous(request.getIsAnonymous())
                .anonymousIdentifier(anonymousIdentifier)
                .build();

        return postRepository.save(post);
    }

    private Mono<PaginatedResponse<PostResponse>> executeGetPostsQuery(
            int page,
            int size,
            UUID threadId,
            UUID authorId,
            UUID parentPostId,
            PostType postType,
            Boolean hasContentWarning,
            Boolean isDeleted,
            String search,
            PostSortField sortBy,
            String sortDirection,
            ViewerContext viewerContext
    ) {
        if (page < 0 || size <= 0) {
            log.error("Invalid pagination parameters: page = {}, size = {}", page, size);
            throw new InvalidPaginationException();
        }

        int offset = page * size;

        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        boolean isAdmin = viewerContext.isAdmin();
        boolean isModeratorOrAdmin = viewerContext.isModeratorOrAdmin();
        boolean isVerified = viewerContext.isVerified();

        String effectivePostType = (postType == null) ? null : postType.name();
        String effectiveSearch = (search == null || search.isBlank()) ? null : search.trim();

        PostSortField sortByField = sortBy != null? sortBy : PostSortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);

        // Note: flaggedForReview is not currently used as a filter in the service

        return postRepository.findPostsPaginated(
                        viewerId,
                        isAdmin, isModeratorOrAdmin, isVerified,
                        threadId, authorId, parentPostId,
                        effectivePostType, hasContentWarning, isDeleted, false,
                        effectiveSearch,
                        sortByField.getValue(), effectiveSortDirection,
                        size, offset)
                .collectList()
                .zipWith(postRepository.countPostsWithFilters(
                        viewerId,
                        isAdmin, isModeratorOrAdmin, isVerified,
                        threadId, authorId, parentPostId,
                        effectivePostType, hasContentWarning, isDeleted, false,
                        effectiveSearch))
                .flatMap(tuple -> {
                    List<PostEntity> posts = tuple.getT1();
                    long totalCount = tuple.getT2();

                    if(posts.isEmpty()){
                        FilterMetadata<Object> filters = FilterMetadata.builder()
                                .sortOptions(getPostSortOptions())
                                .build();
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, totalCount, filters));
                    }

                    return enrichPostsWithBatchData(posts)
                            .map(enriched -> {

                                FilterMetadata<PostFilterDto> filters = buildPostFilters(enriched);
                                return new PaginatedResponse<>(enriched.responses, page, size, totalCount, filters);
                            });
                });

    }

    private PostSortField validateAndNormalizeSortBy(String sortBy) {
      return PostSortField.fromString(sortBy);
    }

    private record ValidationRule(Predicate<PostEntity> condition, String errorMessage, ErrorCode errorCode) {
        public ValidationRule(Predicate<PostEntity> condition, String errorMessage) {
            this(condition, errorMessage, ErrorCode.VALIDATION_FAILED);
        }
    }

    private <T> Mono<T> performUserAction(
            UUID postId,
            ViewerContext viewerContext,
            String actionDescription,
            Function<PostEntity, Mono<T>> action,
            Predicate<PostEntity> validator,
            String validationErrorMessage
    ) {
        return findActivePost(postId, viewerContext)
                .flatMap(post -> {
                    if (!post.getAuthorId().toString().equals(viewerContext.getUserId())) {
                        return Mono.error(new ApiException("Only post author can " + actionDescription, ErrorCode.FORBIDDEN));
                    }
                    if (validator != null && !validator.test(post)) {
                        return Mono.error(new ApiException(validationErrorMessage, ErrorCode.VALIDATION_FAILED));
                    }
                    return action.apply(post);
                })
                .as(transactionalOperator::transactional);
    }

    private <T> Mono<T> performModeratorAction(
            UUID postId,
            ViewerContext viewerContext,
            Function<PostEntity, Mono<T>> action,
            List<ValidationRule> validators,
            boolean requireActive
    ) {
        Mono<PostEntity> postMono = requireActive ? findActivePost(postId, viewerContext) : findPostWithVisibility(postId, viewerContext);

        return postMono
                .flatMap(post -> {
                    for (ValidationRule rule : validators) {
                        if (rule.condition().test(post)) {
                            return Mono.error(new ApiException(rule.errorMessage(), rule.errorCode()));
                        }
                    }
                    return action.apply(post);
                })
                .as(transactionalOperator::transactional);
    }

    /**
     * Enriches a single post with author details.
     * Uses individual queries since only one post is being fetched.
     */
    private Mono<PostResponse> enrichSinglePostWithData(PostEntity post) {

        Mono<ThreadDetails> threadMono = post.getThreadId() != null
                ? threadRepository.findById(post.getThreadId())
                  .map(ThreadEntity::toThreadDetails)
                  .defaultIfEmpty(ThreadEntity.defaultThread())
                : Mono.just(ThreadEntity.defaultThread());

        Mono<PostDetails> parentPostMono = post.getParentPostId() != null
                ? postRepository.findById(post.getParentPostId())
                .map(PostEntity::toPostDetails)
                .defaultIfEmpty(PostEntity.defaultPost())
                : Mono.just(PostEntity.defaultPost());

        // Get all userId maps
        List<UUID> allUserIds = Stream.of(post.getAuthorId(), post.getEditedByUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch all users
        Mono<Map<UUID, UserDetails>> usersMap = appUserRepository
                .findAppUsersByKeycloakIds(allUserIds)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());

        return Mono.zip(
                threadMono,

                parentPostMono,

                usersMap

        ).map(tuple -> {
                ThreadDetails thread = tuple.getT1();
                PostDetails parentPost = tuple.getT2();
                Map<UUID, UserDetails> users = tuple.getT3();

                UserDetails author = users.getOrDefault(post.getAuthorId(), AppUserEntity.defaultUser());
                UserDetails editor = users.getOrDefault(post.getEditedByUserId(), AppUserEntity.defaultUser());

                return mapResponseWithData(post, thread, parentPost, author, editor);
        });

    }

    /**
     * Enriches a list of posts with author details using batch fetching.
     * Uses batch fetching to avoid N+1 queries.
     */
    private Mono<EnrichedPostData> enrichPostsWithBatchData(List<PostEntity> posts){
        if(posts.isEmpty()){
            return Mono.just(new EnrichedPostData(
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of()
            ));
        }

        List<UUID> threadIds = posts.stream()
                .map(PostEntity::getThreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> parentPostIds = posts.stream()
                .map(PostEntity::getParentPostId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> authorIds = posts.stream()
                .map(PostEntity::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> editorIds = posts.stream()
                .map(PostEntity::getEditedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch all threads
        Mono<Map<UUID, ThreadDetails>> threadsMap = threadRepository
                .findThreadsByIds(threadIds)
                .collectMap(ThreadEntity::getId, ThreadEntity::toThreadDetails)
                .defaultIfEmpty(new HashMap<>());

        // Batch fetch all parent posts
        Mono<Map<UUID, PostDetails>> parentPostsMap = postRepository
                .findPostsByIds(parentPostIds)
                .collectMap(PostEntity::getId, PostEntity::toPostDetails)
                .defaultIfEmpty(new HashMap<>());


        List<UUID> allUserIds = posts.stream()
                .flatMap(post -> Stream.of(post.getAuthorId(), post.getEditedByUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch all users
        Mono<Map<UUID, UserDetails>> usersMap = appUserRepository
                .findAppUsersByKeycloakIds(allUserIds)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());


        return Mono.zip(threadsMap, parentPostsMap, usersMap)
                .map(tuple -> {
                    Map<UUID, ThreadDetails> threads = tuple.getT1();
                    Map<UUID, PostDetails> parentPosts = tuple.getT2();
                    Map<UUID, UserDetails> users =  tuple.getT3();

                    List<PostResponse> responses = posts.stream()
                            .map(post -> {

                                ThreadDetails thread = threads.getOrDefault(post.getThreadId(), ThreadEntity.defaultThread());
                                PostDetails parentPost = parentPosts.getOrDefault(post.getParentPostId(), null);
                                UserDetails author = users.getOrDefault(post.getAuthorId(), AppUserEntity.defaultUser());
                                UserDetails editor = users.getOrDefault(post.getEditedByUserId(), null);

                                return mapResponseWithData(post, thread, parentPost, author, editor);
                            })
                            .toList();

                    return new EnrichedPostData(
                            responses,
                            posts,
                            users,
                            threads
                    );
                });

    }

    /**
     * Builds a PostResponse from post and author data.
     * Used by both single and batch enrichment flows.
     */
    private PostResponse mapResponseWithData(
        PostEntity post,
        ThreadDetails thread,
        PostDetails parentPost,
        UserDetails author,
        UserDetails editor
    ) {
        return PostResponse.builder()
                .id(post.getId())

                .thread(thread)
                .parentPost(parentPost)

                .author(author)
                .anonymousIdentifier(post.getAnonymousIdentifier())

                .postType(post.getPostType())
                .content(post.getContent())
                .wordCount(post.getWordCount())

                .contentWarningType(post.getContentWarningType())
                .contentWarningCustomText(post.getContentWarningCustomText())

                .isFlaggedForReview(false)
                .isEdited(post.getIsEdited())
                .editReason(post.getEditReasonType())
                .editReasonCustomText(post.getEditReasonCustomText())
                // .editedAt(post.getEditedAt()) // missing. Should be included in the entity
                .editedBy(editor)

                .isAnonymous(post.getIsAnonymous())
                .isDeleted(post.getIsDeleted())

                .reactionCount(post.getReactionCount())

                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }


    private record EnrichedPostData(
            List<PostResponse> responses,
            List<PostEntity> posts,
            Map<UUID, UserDetails> users,
            Map<UUID, ThreadDetails> threads
    ){}

    /**
     * Builds filter metadata from enriched post data.
     */
    private FilterMetadata<PostFilterDto> buildPostFilters(EnrichedPostData data){
        // Build author options
        Map<UUID, Long> authorCounts = data.posts.stream()
                .collect(Collectors.groupingBy(
                        PostEntity::getAuthorId,
                        Collectors.counting()
                ));

        List<FilterOption> authorOptions = data.users.entrySet().stream()
                .map(entry -> {
                    UUID authorId = entry.getKey();
                    UserDetails author = entry.getValue();
                    long count = authorCounts.getOrDefault(authorId, 0L);
                    return FilterOption.ofUser(
                            authorId,
                            author.getDisplayName(),
                            author.getAvatarUrl(),
                            author.getInitials(),
                            count
                    );
                })
                .filter(option -> option.getCount() > 0)
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();

        // Build thread options
        Map<UUID, Long> threadCounts = data.posts.stream()
                .collect(Collectors.groupingBy(
                        PostEntity::getThreadId,
                        Collectors.counting()
                ));

        List<FilterOption> threadOptions = data.threads.entrySet().stream()
                .map(entry -> {
                    UUID threadId = entry.getKey();
                    ThreadDetails thread = entry.getValue();
                    long count = threadCounts.getOrDefault(threadId, 0L);
                    return FilterOption.ofEntity(
                        threadId,
                        thread.getTitle(),
                        threadId.toString(),
                        count
                    );
                })
                .sorted(Comparator.comparing(FilterOption::getLabel))
                .toList();


        // Build post type options
        Map<PostType, Long> postTypeCounts = data.posts.stream()
                .map(PostEntity::getPostType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        List<FilterOption> postTypeOptions = Arrays.stream(PostType.values())
                .map(postType -> FilterOption.ofEnum(
                        postType.getDisplayName(),
                        postType.name(),
                        postTypeCounts.getOrDefault(postType, 0L)
                ))
                .filter(option -> option.getCount() > 0)
                .toList();

        PostFilterDto postFilters = PostFilterDto.builder()
                .authors(authorOptions)
                .threads(threadOptions)
                .postTypes(postTypeOptions)
                .build();

        return FilterMetadata.<PostFilterDto>builder()
                .filters(postFilters)
                .sortOptions(getPostSortOptions())
                .build();

    }

    private List<SortOption> getPostSortOptions() {
        return Arrays.stream(PostSortField.values())
                .map(PostSortField::toSortOption)
                .toList();
    }

}