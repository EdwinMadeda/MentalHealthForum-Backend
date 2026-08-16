package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.OtpResult;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OtpPurpose;
import reactor.core.publisher.Mono;

public interface OtpWorker {

    Mono<OtpResult> generateAndSaveOtp(String email, OtpPurpose purpose);

    Mono<Void> verifyOtp(String email, String rawCode, OtpPurpose purpose);
}
