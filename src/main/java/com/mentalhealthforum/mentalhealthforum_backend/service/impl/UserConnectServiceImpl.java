package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.PaginatedResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.discovery.UserConnectResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.FilterMetadata;
import com.mentalhealthforum.mentalhealthforum_backend.dto.filters.SortOption;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.user.UserDetails;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ConnectionStatus;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ConnectionType;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.enums.listings.ConnectionSortField;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidPaginationException;
import com.mentalhealthforum.mentalhealthforum_backend.model.AppUserEntity;
import com.mentalhealthforum.mentalhealthforum_backend.model.UserConnectEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.repository.UserConnectRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class UserConnectServiceImpl implements UserConnectService {

    private static final Logger log = LoggerFactory.getLogger(UserConnectServiceImpl.class);

    private final TransactionalOperator transactionalOperator;
    private final UserConnectRepository userConnectRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserServiceImpl appUserService;


    public UserConnectServiceImpl(
            TransactionalOperator transactionalOperator,
            UserConnectRepository userConnectRepository,
            AppUserRepository appUserRepository,
            AppUserServiceImpl appUserService) {
        this.transactionalOperator = transactionalOperator;
        this.userConnectRepository = userConnectRepository;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
    }

    @Override
    public Mono<UserConnectResponse> requestConnection(UUID targetUserId, ViewerContext viewerContext){
        UUID viewerId = UUID.fromString(viewerContext.getUserId());

        if(viewerId.equals(targetUserId)){
            return Mono.error(new ApiException("Cannot request connection with yourself", ErrorCode.VALIDATION_FAILED));
        }

        // Order for storage (user1 < user2)
        OrderedPair pair = OrderedPair.of(viewerId, targetUserId);

        return validateUserExists(targetUserId)
                .then(checkOrCreateConnection(pair, viewerId))
                .flatMap(connection -> enrichSingleConnectionWithData(connection, viewerId))
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<UserConnectResponse> acceptConnection(UUID user1, ViewerContext viewerContext){
        UUID user2 = UUID.fromString(viewerContext.getUserId());

        OrderedPair pair = OrderedPair.of(user1, user2);
        UUID finalRequesterId = pair.user1;
        UUID finalReceiverId = pair.user2;

        return userConnectRepository.findByUser1AndUser2(finalRequesterId, finalReceiverId)
                .switchIfEmpty(Mono.error(new ApiException("Connection request not found", ErrorCode.VALIDATION_FAILED)))
                .flatMap(connection -> {
                    if(connection.getStatus() != ConnectionStatus.PENDING){
                        return Mono.error(new ApiException("Connection request already processed", ErrorCode.VALIDATION_FAILED));
                    }
                    connection.setStatus(ConnectionStatus.ACCEPTED);
                    connection.setUpdatedAt(Instant.now());
                    return userConnectRepository.save(connection);
                })
                .flatMap(connection -> enrichSingleConnectionWithData(connection, user2))
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<Void> declineConnection(UUID user1, ViewerContext viewerContext){
        UUID user2 = UUID.fromString(viewerContext.getUserId());

        OrderedPair pair = OrderedPair.of(user1, user2);
        UUID finalRequesterId = pair.user1;
        UUID finalReceiverId = pair.user2;

        return userConnectRepository.findByUser1AndUser2(finalRequesterId, finalReceiverId)
                .switchIfEmpty(Mono.error(new ApiException("Connection request not found", ErrorCode.VALIDATION_FAILED)))
                .flatMap(connection -> {
                    if(connection.getStatus() != ConnectionStatus.PENDING){
                        return Mono.error(new ApiException("Connection request already processed", ErrorCode.VALIDATION_FAILED));
                    }
                    connection.setStatus(ConnectionStatus.DECLINED);
                    connection.setUpdatedAt(Instant.now());
                    return userConnectRepository.save(connection);
                })
                .then()
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<Void> terminateConnection(UUID connectedUserId, ViewerContext viewerContext){
        UUID userId = UUID.fromString(viewerContext.getUserId());

        OrderedPair pair = OrderedPair.of(connectedUserId, userId);

        return userConnectRepository.findByUser1AndUser2(pair.user1, pair.user2)
                .switchIfEmpty(Mono.error(new ApiException("Connection request not found", ErrorCode.VALIDATION_FAILED)))
                .flatMap(connection -> {
                    if(connection.getStatus() != ConnectionStatus.ACCEPTED){
                        return Mono.error(new ApiException("Connection is not active", ErrorCode.VALIDATION_FAILED));
                    }
                    return userConnectRepository.deleteByUser1AndUser2(pair.user1, pair.user2);
                })
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<Boolean> areConnected(UUID userId1, UUID userId2){
        // Ensure consistent ordering
        UUID finalUser1 = userId1.compareTo(userId2) < 0? userId1 : userId2;
        UUID finalUser2 = userId1.compareTo(userId2) < 0? userId2 : userId1;

        return userConnectRepository.findByUser1AndUser2(finalUser1, finalUser2)
                .map(connection -> connection.getStatus() == ConnectionStatus.ACCEPTED)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<PaginatedResponse<UserConnectResponse>> getMyConnections(
            int page,
            int size,
            Boolean notificationEnabled,
            String search,
            ConnectionSortField sortBy,
            String sortDirection,
            ViewerContext viewerContext){

        if(page <0 || size <= 0){
            throw new InvalidPaginationException();
        }

        int offset = page * size;
        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        String effectiveSearch = (search == null || search.isBlank()) ? null : search.trim();

        ConnectionSortField sortByField = sortBy != null? sortBy : ConnectionSortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);

        return userConnectRepository.findAcceptedConnectionsPaginated(
                    viewerId,
                    notificationEnabled,
                    effectiveSearch,
                    sortByField.getValue(), effectiveSortDirection,
                    size, offset
                )
                .collectList()
                .zipWith(userConnectRepository.countAcceptedConnectionsWithFilters(viewerId, notificationEnabled, effectiveSearch))
                .flatMap(tuple -> {
                    List<UserConnectEntity> connections = tuple.getT1();
                    long totalCount = tuple.getT2();

                    FilterMetadata<Object> filters = FilterMetadata.builder()
                            .sortOptions(getConnectionSortOptions())
                            .build();

                    if(connections.isEmpty()){
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, totalCount, filters));
                    }

                    return enrichConnectionsWithBatchData(connections, viewerId)
                            .map(enriched ->
                                    new PaginatedResponse<>(enriched, page, size, totalCount, filters)
                            );

                });

    }

    @Override
    public Mono<PaginatedResponse<UserConnectResponse>> getMyPendingRequests(
            int page,
            int size,
            String search,
            ConnectionType connectionType,
            ConnectionSortField sortBy,
            String sortDirection,
            ViewerContext viewerContext
    ){

        if(page <0 || size <= 0){
            throw new InvalidPaginationException();
        }

        int offset = page * size;
        UUID viewerId = UUID.fromString(viewerContext.getUserId());
        String effectiveSearch = (search == null || search.isBlank()) ? null : search.trim();

        ConnectionSortField sortByField = sortBy != null? sortBy : ConnectionSortField.DEFAULT;
        String effectiveSortDirection = sortByField.determineSortDirection(sortDirection);


        return userConnectRepository.findPendingRequestsPaginated(
                        viewerId,
                        effectiveSearch, connectionType.name(),
                        sortByField.getValue(), effectiveSortDirection,
                        size, offset
                )
                .collectList()
                .zipWith(userConnectRepository.countPendingRequestsWithFilters(viewerId, effectiveSearch, connectionType.name()))
                .flatMap(tuple -> {
                    List<UserConnectEntity> connections = tuple.getT1();
                    long totalCount = tuple.getT2();

                    FilterMetadata<Object> filters = FilterMetadata.builder()
                            .sortOptions(getConnectionSortOptions())
                            .build();

                    if(connections.isEmpty()){
                        return Mono.just(new PaginatedResponse<>(List.of(), page, size, totalCount, filters));
                    }

                    return enrichConnectionsWithBatchData(connections, viewerId)
                            .map(enriched ->
                                    new PaginatedResponse<>(enriched, page, size, totalCount, filters)
                            );
                });

    }

    @Override
    public Mono<Long> getConnectionCount(UUID userId){
        return userConnectRepository.countAcceptedConnections(userId);
    }

    @Override
    public Mono<Long> getPendingRequestsCount(UUID userId){
        return userConnectRepository.countIncomingRequests(userId, ConnectionStatus.PENDING);
    }


    // ==================== PRIVATE HELPERS ====================

    private record OrderedPair(UUID user1, UUID user2, boolean isOriginal){
        static OrderedPair of(UUID userId1, UUID userId2){

            // Native 128-bit unsigned bitwise evaluation matches PostgreSQL sorting native
            int comparison = userId1.compareTo(userId2);
            if(comparison < 0){
                // userId1 is smaller, userId2 is larger
                return new OrderedPair(userId1, userId2, true);
            }
            else{
                // userId2 is smaller, userId1 is larger - SWAP!
                return new OrderedPair(userId2, userId1, false);
            }
        }
    }

    private Mono<Void> validateUserExists(UUID userId) {
        return appUserRepository.findAppUserByKeycloakId(userId.toString())
                .switchIfEmpty(Mono.error(new ApiException("User not found", ErrorCode.RESOURCE_NOT_FOUND)))
                .then();
    }

    private Mono<UserConnectEntity> checkOrCreateConnection(OrderedPair pair, UUID initiatedBy) {
        return userConnectRepository.findByUser1AndUser2(pair.user1, pair.user2)
                .flatMap(existing -> {
                    if(existing.getStatus() == ConnectionStatus.ACCEPTED){
                        return Mono.error(new ApiException("Already connected", ErrorCode.VALIDATION_FAILED));
                    }

                    if(existing.getStatus() == ConnectionStatus.PENDING){
                        // Check if this is a mutual request (the other user requested first)
                        if(!existing.getInitiatedBy().equals(initiatedBy)){
                            // Mutual request! Auto-accept
                            existing.setStatus(ConnectionStatus.ACCEPTED);
                            existing.setUpdatedAt(Instant.now());
                            return userConnectRepository.save(existing);
                        }
                        return Mono.error(new ApiException("Connection request already sent", ErrorCode.VALIDATION_FAILED));
                    }

                    // DISMISSED state - cannot re-request
                    return Mono.error(new ApiException("Cannot request connection if the last one was declineed", ErrorCode.VALIDATION_FAILED));
                })
                .switchIfEmpty(Mono.defer(()-> createConnection(pair, initiatedBy)));
    }

    private Mono<UserConnectEntity> createConnection(OrderedPair pair, UUID initiatedBy) {
        UserConnectEntity connection = UserConnectEntity.builder()
                .user1(pair.user1)
                .user2(pair.user2)
                .initiatedBy(initiatedBy)
                .status(ConnectionStatus.PENDING)
                .notificationEnabled(false)
                .createdAt(Instant.now())
                .build();
        return userConnectRepository.save(connection);
    }

    /**
     * Enriches a single connection with user details.
     * Uses individual queries since only one connection is being fetched.
     */
    private Mono<UserConnectResponse> enrichSingleConnectionWithData(UserConnectEntity connection, UUID viewerId) {
        UUID initiatedById = connection.getInitiatedBy();
        UUID recipientId = connection.getRecipient();

        return Mono.zip(
                appUserService.getUserDetails(initiatedById),
                appUserService.getUserDetails(recipientId)
        ).map(tuple -> {
            UserDetails initiatorDetails = tuple.getT1();
            UserDetails recipientDetails = tuple.getT2();

            return  mapResponseWithData(connection, initiatorDetails, recipientDetails, viewerId);
        });
    }

    /**
     * Enriches a list of connections with user details using batch fetching.
     * Uses batch fetching to avoid N+1 queries.
     */
    private Mono<List<UserConnectResponse>> enrichConnectionsWithBatchData(
        List<UserConnectEntity> connections,
        UUID viewerId){

        if(connections.isEmpty()){
            return Mono.just(List.of());
        }

        // Extract all user IDs that need to be fetched
        Set<UUID> userIds = new HashSet<>();
        for(UserConnectEntity connection : connections){
            userIds.add(connection.getInitiatedBy());
            userIds.add(connection.getRecipient());
        }

        List<UUID> userIdList = new ArrayList<>(userIds);

        // Batch fetch all user details
        Mono<Map<UUID, UserDetails>> userDetailsMap = appUserRepository
                .findAppUsersByKeycloakIds(userIdList)
                .collectMap(AppUserEntity::getKeycloakId, AppUserEntity::toUserDetails)
                .defaultIfEmpty(new HashMap<>());

        return userDetailsMap
                .map(userDetails -> {
                    return connections.stream()
                            .map(connection -> {
                                UUID initiatedBy = connection.getInitiatedBy();
                                UUID recipientId = connection.getRecipient();

                                UserDetails initiatorDetails = userDetails.get(initiatedBy);
                                UserDetails recipientDetails = userDetails.get(recipientId);

                                return mapResponseWithData(connection, initiatorDetails, recipientDetails, viewerId);

                            })
                            .toList();
                });

    }

    /**
     * Builds a UserConnectResponse from connection and user details.
     * Used by both single and batch enrichment flows.
     */
    private UserConnectResponse mapResponseWithData(
            UserConnectEntity connection,
            UserDetails initiatedBy,
            UserDetails recipient,
            UUID viewerId){

        ConnectionType connectionType = connection.getInitiatedBy().equals(viewerId)
                ? ConnectionType.OUTGOING
                : ConnectionType.INCOMING;

        return  UserConnectResponse.builder()
                .id(connection.getId())
                .connectionStatus(connection.getStatus())
                .notificationEnabled(connection.getNotificationEnabled())
                .createdAt(connection.getCreatedAt())

                // Initiator details
                .initiatedBy(initiatedBy)

                // Recipient details
                .recipient(recipient)

                .connectionType(connectionType)

                .build();
    }

    private List<SortOption> getConnectionSortOptions(){
        return Arrays.stream(ConnectionSortField.values())
                .map(ConnectionSortField::toSortOption)
                .toList();
    }

}
