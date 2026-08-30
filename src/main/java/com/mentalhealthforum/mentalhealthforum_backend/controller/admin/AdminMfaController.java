package com.mentalhealthforum.mentalhealthforum_backend.controller.admin;

import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.dto.ViewerContext;
import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.*;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.service.JwtClaimsExtractor;
import com.mentalhealthforum.mentalhealthforum_backend.service.MfaService;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.MfaStateCache;
import com.mentalhealthforum.mentalhealthforum_backend.utils.SecureCookieUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/mfa")
public class AdminMfaController {

    private static final Logger log = LoggerFactory.getLogger(AdminMfaController.class);

    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final MfaService mfaService;
    private final MfaStateCache mfaStateCache;
    private final SecureCookieUtils cookieUtils;

    public AdminMfaController(JwtClaimsExtractor jwtClaimsExtractor, MfaService mfaService, SecureCookieUtils cookieUtils, MfaStateCache mfaStateCache) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.mfaService = mfaService;
        this.mfaStateCache = mfaStateCache;
        this.cookieUtils = cookieUtils;
    }

    @PostMapping("/setup")
    public Mono<ResponseEntity<StandardSuccessResponse<MfaSetupResponse>>> setupMfa(
            @AuthenticationPrincipal Jwt jwt,
            ServerHttpRequest httpRequest,
            ServerHttpResponse httpResponse){
        // Admin must be authenticated
        // Send OTP to admin's email
        // Return setup info

        ViewerContext viewerContext = jwtClaimsExtractor.extractViewerContext(jwt);
        String userId = viewerContext.getUserId();

        String ipAddress = httpRequest.getRemoteAddress() != null ?
                httpRequest.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        String userAgent = httpRequest.getHeaders().getFirst("User-agent");

        return mfaService.setupMfa(userId, ipAddress, userAgent)
                .map(result -> {
                    cookieUtils.setMfaStateCookie(
                            httpResponse,
                            result.stateToken(),
                            result.expirySeconds()
                    );
                    return ResponseEntity.ok(new StandardSuccessResponse<>("OTP sent to your email.", result.toResponse()));
                });
    }

    @PostMapping("/confirm")
    public Mono<ResponseEntity<StandardSuccessResponse<MfaConfirmResponse>>> confirmMfa(
            @Valid @RequestBody MfaConfirmRequest request,
            ServerHttpRequest httpRequest,
            ServerHttpResponse httpResponse){
        // Validate OTP
        // Enable MFA
        // Generate + return backup codes

        // Validate the state token and OTP
        return cookieUtils.getMfaStateToken(httpRequest)
                .switchIfEmpty(Mono.error(new ApiException(
                        "No MFA session found. Please log in again.",
                        ErrorCode.UNAUTHORIZED
                )))
                .flatMap(stateToken ->
                        mfaService.verifyMfaOtp(stateToken, request.otpCode())
                        .flatMap(mfaService::enableMfa)
                        .map(appUser -> {

                            cookieUtils.clearMfaStateTokenCookie(httpResponse);

                            mfaStateCache.invalidate(stateToken);

                            MfaConfirmResponse response = new MfaConfirmResponse(
                                    true,
                                    appUser.getRawMfaBackupCodes()  // Backup codes for the admin to save
                            );
                            return ResponseEntity.ok(new StandardSuccessResponse<>("MFA enabled successfully.", response));
                        })
                );

    }

}
