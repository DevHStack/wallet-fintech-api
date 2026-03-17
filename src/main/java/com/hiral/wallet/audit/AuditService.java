package com.hiral.wallet.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void record(String userId, Long accountId, String action, String details) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .accountId(accountId)
                .action(action)
                .details(details)
                .build();
        repository.save(log);
    }
}
