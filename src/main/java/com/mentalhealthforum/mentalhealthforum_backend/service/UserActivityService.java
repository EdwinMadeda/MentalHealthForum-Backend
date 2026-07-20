package com.mentalhealthforum.mentalhealthforum_backend.service;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserActivityService {
    Mono<Void> recordLoginActivity(UUID userId);

    Mono<Void> trackActivity(UUID userId);

    Mono<Void> activateUser(UUID userId);

    Mono<Void> deactivateUser(UUID userId);
}
