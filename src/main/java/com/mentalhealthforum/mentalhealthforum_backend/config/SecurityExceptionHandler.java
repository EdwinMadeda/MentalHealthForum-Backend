package com.mentalhealthforum.mentalhealthforum_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentalhealthforum.mentalhealthforum_backend.contants.AppConstants;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardErrorResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.repository.AppUserRepository;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserModerationService;
import com.mentalhealthforum.mentalhealthforum_backend.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;


/**
 * Reactive exception handler for 401 (Authentication) and 403 (Authorization) errors.
 * Implements reactive interfaces ServerAuthenticationEntryPoint and ServerAccessDeniedHandler.
 */
@Component
public class SecurityExceptionHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final UserModerationService userModerationService;
    private final AppUserRepository appUserRepository;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public SecurityExceptionHandler(
            ObjectMapper objectMapper,
            UserModerationService userModerationService,
            AppUserRepository appUserRepository,
            JwtClaimsExtractor jwtClaimsExtractor) {
        this.objectMapper = objectMapper;
        this.userModerationService = userModerationService;
        this.appUserRepository = appUserRepository;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }


    // --- Handles 401 Unauthorized (Authentication Failure) ---
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        logger.warn("401 Unauthorized Access Attempt: {}", ex.getMessage());
        return writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Authentication failed. Invalid or missing credentials."
        );
    }

    // --- Handles 403 Forbidden (Authorization Failure) ---
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        logger.warn("403 Forbidden Access Attempt: {}", ex.getMessage());

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuthenticationToken -> {
                    UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
                    ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwtAuthenticationToken.getToken());


                    // Check account status first
                    return appUserRepository.findAppUserByKeycloakId(userId.toString())
                            .flatMap(appUser -> {
                                Instant scheduledAt = appUser.getDeletionScheduledAt();
                                String readableDate = DateTimeUtils.toHumanReadable(
                                        scheduledAt,
                                        String.format("within %s days", AppConstants.ACCOUNT_DELETION_RETENTION_WINDOW)
                                );

                                // Block access if account is pending deletion or purged
                                if(appUser.isPendingDeletion()){
                                    return writeErrorResponse(
                                            exchange,
                                            HttpStatus.FORBIDDEN,
                                            ErrorCode.ACCOUNT_PENDING_DELETION,
                                            String.format(
                                                    "Account deletion is scheduled for %s. You can reactivate your account by visiting your account settings",
                                                    readableDate
                                            )
                                    );
                                }
                                if(appUser.isPurged()){
                                    return writeErrorResponse(
                                            exchange,
                                            HttpStatus.FORBIDDEN,
                                            ErrorCode.ACCOUNT_PURGED,
                                            "Account has been permanently deleted. This action cannot be undone"
                                    );
                                }

                                // Check ban next (most severe)
                                return userModerationService.isUserBanned(userId)
                                        .flatMap(isBanned -> {
                                            if(isBanned){
                                                return userModerationService.getActiveBanForUser(userId, viewerContext
                                                        )
                                                        .flatMap(ban -> writeErrorResponse(
                                                                exchange,
                                                                HttpStatus.FORBIDDEN,
                                                                ErrorCode.FORBIDDEN,
                                                                String.format(
                                                                        "Your account has been permanently banned. Reason: %s. Please contact support.",
                                                                        ban.getReason()
                                                                )
                                                        ));
                                            }

                                            // Not banned, check suspension
                                            return userModerationService.isUserSuspended(userId)
                                                    .flatMap(isSuspended -> {
                                                        if(isSuspended) {
                                                            return userModerationService.getActiveSuspendForUser(userId, viewerContext)
                                                                    .flatMap(suspension -> {
                                                                        String expiry = DateTimeUtils.toHumanReadable(suspension.getExpiresAt(), "indefinitely");
                                                                        return writeErrorResponse(
                                                                                exchange,
                                                                                HttpStatus.FORBIDDEN,
                                                                                ErrorCode.FORBIDDEN,
                                                                                String.format(
                                                                                        "Your account is suspended until %s. Reason: %s.",
                                                                                        expiry, suspension.getReason()
                                                                                )
                                                                        );
                                                                    });
                                                        }

                                                        // Check onboarding
//                                                        boolean isOnboarding = jwtAuthenticationToken.getAuthorities().stream()
//                                                                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ONBOARDING"));

                                                        if (appUser.isOnboarding()) {
                                                            return writeErrorResponse(
                                                                    exchange,
                                                                    HttpStatus.PRECONDITION_REQUIRED,
                                                                    ErrorCode.ONBOARDING_REQUIRED,
                                                                    "Your profile is incomplete. Please satisfy onboarding requirements first."
                                                            );
                                                        }

                                                        // Default 403
                                                        return writeErrorResponse(
                                                                exchange,
                                                                HttpStatus.FORBIDDEN,
                                                                ErrorCode.FORBIDDEN,
                                                                "Forbidden: Insufficient permissions for this resource."
                                                        );
                                                    });
                                        });
                            });



                })
                // If anonymous (unauthenticated) reached here, it's a 403
                .switchIfEmpty(writeErrorResponse(
                        exchange,
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "Forbidden: Authentication required."
                ));
    }


    /**
     * Sets headers, creates the custom DTO, writes JSON to the response body, and completes the Mono.
     */
    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus httpStatus,
            ErrorCode errorCode,
            String message
    ) {
        // 1. Set Status and Headers
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setAccessControlAllowOrigin("*"); // Important for CORS errors

        // 2. Create the DTO
        StandardErrorResponse errorResponse = new StandardErrorResponse(
                message,
                errorCode,
                exchange.getRequest().getPath().toString(),
                null
        );

        // 3. Serialize DTO to JSON and write to response body
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            logger.error("Failed to write custom error response for status {}: {}", httpStatus, e.getMessage());
            return Mono.error(e);
        }
    }
}