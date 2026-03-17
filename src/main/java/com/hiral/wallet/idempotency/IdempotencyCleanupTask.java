package com.hiral.wallet.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IdempotencyCleanupTask {

    private final IdempotencyRecordRepository repository;

    @Value("${idempotency.cleanup-interval-ms:3600000}")
    private long cleanupIntervalMs;

    @Scheduled(fixedDelayString = "${idempotency.cleanup-interval-ms:3600000}")
    public void cleanupExpired() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
