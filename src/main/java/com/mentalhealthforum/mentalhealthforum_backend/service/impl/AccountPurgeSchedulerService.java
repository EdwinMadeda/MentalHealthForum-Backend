package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import reactor.core.publisher.Mono;

public interface AccountPurgeSchedulerService {
    Mono<Integer> purgeExpiredAccounts();
}
