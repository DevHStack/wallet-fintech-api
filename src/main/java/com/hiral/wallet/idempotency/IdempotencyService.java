package com.hiral.wallet.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    public String computeHash(Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute idempotency hash", e);
        }
    }

    @Transactional
    public <T> T execute(
            String userId,
            String idempotencyKey,
            String endpoint,
            Object requestPayload,
            Class<T> responseType,
            Supplier<T> action) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String storedKey = buildStoredKey(userId, idempotencyKey, endpoint);
        String requestHash = computeHash(requestPayload);
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(storedKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key already used with a different request");
            }

            if (responseType == Void.class) {
                return null;
            }

            try {
                return objectMapper.readValue(record.getResponsePayload(), responseType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize idempotent response", e);
            }
        }

        T result = action.get();
        String responsePayload = null;
        if (responseType != Void.class) {
            try {
                responsePayload = objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize idempotent response", e);
            }
        }

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(storedKey)
                .userId(userId)
                .requestHash(requestHash)
                .endpoint(endpoint)
                .responsePayload(responsePayload)
                .expiresAt(LocalDateTime.now().plusSeconds(ttlSeconds))
                .build();

        try {
            repository.save(record)  ;
        } catch (DataIntegrityViolationException e) {
            // Race: another thread inserted the same key first; load and replay
            IdempotencyRecord replay = repository.findByIdempotencyKey(storedKey)
                    .orElseThrow(() -> new RuntimeException("Idempotency record disappeared"));

            if (!replay.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key already used with a different request");
            }

            if (responseType == Void.class) {
                return null;
            }

            try {
                return objectMapper.readValue(replay.getResponsePayload(), responseType);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Failed to deserialize idempotent response", ex);
            }
        }

        return result;
    }

    private String buildStoredKey(String userId, String idempotencyKey, String endpoint) {
        return String.format("%s:%s:%s", userId == null ? "" : userId, endpoint, idempotencyKey);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
