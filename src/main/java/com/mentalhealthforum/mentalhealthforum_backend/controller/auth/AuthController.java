package com.mentalhealthforum.mentalhealthforum_backend.controller.auth;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.*;
import com.mentalhealthforum.mentalhealthforum_backend.dto.StandardSuccessResponse;
import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.ApiException;
import com.mentalhealthforum.mentalhealthforum_backend.service.AuthService;
import com.mentalhealthforum.mentalhealthforum_backend.service.MfaService;
import com.mentalhealthforum.mentalhealthforum_backend.service.UserService;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.MfaStateCache;
import com.mentalhealthforum.mentalhealthforum_backend.utils.SecureCookieUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final MfaService mfaService;
    private final MfaStateCache mfaStateCache;
    private final SecureCookieUtils cookieUtils;

    public AuthController(AuthService authService, UserService userService, MfaService mfaService, MfaStateCache mfaStateCache, SecureCookieUtils cookieUtils) {
        this.authService = authService;
        this.userService = userService;
        this.mfaService = mfaService;
        this.mfaStateCache = mfaStateCache;
        this.cookieUtils = cookieUtils;
    }

    // -------------------------------------------------------------------------
    // MANUAL AUTHENTICATION AND TOKEN LIFECYCLE (Already Reactive)
    // -------------------------------------------------------------------------

    /**
     * Handles manual user login (ROPC Grant). Returns Access and Refresh Tokens.
     * The service returns Mono<JwtResponse>, which is mapped to a 200 OK Response.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<StandardSuccessResponse<Object>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            ServerHttpRequest httpRequest,
            ServerHttpResponse httpResponse
    ) {

        String ipAddress = httpRequest.getRemoteAddress() != null ?
                httpRequest.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        String userAgent = httpRequest.getHeaders().getFirst("User-agent");

        return authService.authenticate(loginRequest, ipAddress, userAgent)
                .map(result -> {
                    if(result instanceof AuthResult.Success(JwtResponse jwt)){
                        // Uses SecureCookieUtils to set the tokens, encoding the refresh token value
                        cookieUtils.setTokenCookies(
                                httpResponse,
                                jwt.accessToken(),
                                jwt.refreshToken(),
                                jwt.expiresIn(),
                                jwt.refreshExpiresIn());


                        return ResponseEntity.ok(new StandardSuccessResponse<>("Login successful."));
                    }
                    else if(result instanceof AuthResult.MfaRequired(
                            String stateToken, String email, int otpLength, int expirySeconds
                    )){
                        cookieUtils.setMfaStateCookie(
                                httpResponse,
                                stateToken,
                                expirySeconds
                        );

                        MfaChallengeResponse data = new MfaChallengeResponse(
                                true,
                                email,
                                otpLength,
                                expirySeconds
                        );

                        return ResponseEntity.ok(new StandardSuccessResponse<>("MFA required. Please enter the code sent to your email.", data));
                    }

                    return ResponseEntity.badRequest().build();
                });

    }

    @PostMapping("/mfa/verify")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            ServerHttpRequest httpRequest,
            ServerHttpResponse httpResponse){
        // Validate state token (from cookie)
        // Validate OTP
        // Issue full Keycloak tokens
        // Set cookies
        // Clear state token

        return cookieUtils.getMfaStateToken(httpRequest)
                .switchIfEmpty(Mono.error(new ApiException(
                        "No MFA session found. Please log in again.",
                        ErrorCode.UNAUTHORIZED
                )))
                .flatMap(stateToken -> {
                    return mfaService.verifyMfaOtp(stateToken, request.otpCode())
                            .flatMap(appUser -> {
                                return authService.issueFullTokens(stateToken)
                                        .flatMap(jwtResponse ->  {
                                            cookieUtils.setTokenCookies(
                                                    httpResponse,
                                                    jwtResponse.accessToken(),
                                                    jwtResponse.refreshToken(),
                                                    jwtResponse.expiresIn(),
                                                    jwtResponse.refreshExpiresIn());

                                            cookieUtils.clearMfaStateTokenCookie(httpResponse);
                                            // Burn the state token immediately
                                            // so it cannot be intercepted or replayed by a malicious actor.
                                            mfaStateCache.invalidate(stateToken);

                                            return Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Login successful.")));
                                        });
                            });
                });
    }

    @PostMapping("/mfa/recovery")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> recoverWithBackupCode(
            @Valid @RequestBody MfaRecoveryRequest request,
            ServerHttpRequest httpRequest,
            ServerHttpResponse httpResponse){

        return cookieUtils.getMfaStateToken(httpRequest)
                .switchIfEmpty(Mono.error(new ApiException(
                        "No MFA session found. Please log in again.",
                        ErrorCode.UNAUTHORIZED
                )))
                .flatMap(stateToken -> {
                    return mfaService.recoverWithBackupCode(stateToken, request.backupCode())
                            .then(authService.issueFullTokens(stateToken)
                                    .flatMap(jwtResponse -> {
                                        cookieUtils.setTokenCookies(
                                                httpResponse,
                                                jwtResponse.accessToken(),
                                                jwtResponse.refreshToken(),
                                                jwtResponse.expiresIn(),
                                                jwtResponse.refreshExpiresIn());

                                        cookieUtils.clearMfaStateTokenCookie(httpResponse);

                                        return Mono.just(ResponseEntity.ok(new StandardSuccessResponse<>("Backup code verified. Please reconfigure MFA.")));
                                    }));

                });

    }



    /**
     * Exchanges an expired refresh token for a new access and refresh token pair.
     * The service returns Mono<JwtResponse>, which is mapped to a 200 OK Response.
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<StandardSuccessResponse<Object>>> refresh(
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        // Use SecureCookieUtils to extract and decode the refresh token from the cookie
        return cookieUtils.getDecodedRefreshToken(request)
                .flatMap(authService::refreshTokens)
                .map(jwt ->{
                    // Use SecureCookieUtils to set the new tokens
                    cookieUtils.setTokenCookies(
                            response,
                            jwt.accessToken(),
                            jwt.refreshToken(),
                            jwt.expiresIn(),
                            jwt.refreshExpiresIn());

                    return ResponseEntity.ok(new StandardSuccessResponse<>("Refresh successful."));
                });
    }
    /**
     * AppUserEntity logout (Token Revocation).
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> logoutUser(
            ServerHttpRequest request,
            ServerHttpResponse response
    ){
        // Use SecureCookieUtils to extract and decode the refresh token from the cookie
        // If the cookie is missing, we still clear the cookies and report Success.
        Mono<String> refreshTokenMono = cookieUtils.getDecodedRefreshToken(request)
                .onErrorResume(IllegalStateException.class, e -> Mono.empty()); // Return empty Mono if cookie is missing

        return refreshTokenMono
                .flatMap(authService::logout)
                .doOnTerminate(() -> cookieUtils.clearTokenCookies(response)) // Clear cookies regardless of logout Success/failure
                .thenReturn(ResponseEntity.ok(
                        new StandardSuccessResponse<>(
                                "Logout successful."
                        )
                ));
    }

    @PostMapping("/forgot-password/initiate")
    public Mono<ResponseEntity<StandardSuccessResponse<ForgotPasswordInitResponse>>> initiateForgotPassword(@Valid @RequestBody ForgotPasswordInitRequest request){
        return userService.initiateForgotPassword(request.email())
                .map(response ->
                        ResponseEntity.ok(new StandardSuccessResponse<>("If an account exists, a reset code has been sent.", response)));

    }

    @PostMapping("/forgot-password/complete")
    public Mono<ResponseEntity<StandardSuccessResponse<Void>>> completeForgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        return userService.completeForgotPassword(request)
                .thenReturn(ResponseEntity.ok(new StandardSuccessResponse<>("Password reset successfully")));
    }

}
