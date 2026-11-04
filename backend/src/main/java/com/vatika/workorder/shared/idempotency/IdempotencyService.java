package com.vatika.workorder.shared.idempotency;

import tools.jackson.databind.json.JsonMapper;
import com.vatika.workorder.shared.exception.DuplicateResourceException;
import com.vatika.workorder.shared.exception.IdempotencyKeyMismatchException;
import com.vatika.workorder.shared.tenancy.TenantContext;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final JsonMapper jsonMapper;
    private final WorkOrderService workOrderService;

    @Transactional
    public WorkOrderResponse create(String idempotencyKey, CreateWorkOrderRequest request) {
        UUID clientId = TenantContext.require();
        String requestHash = hash(request);
        IdempotencyKeyId idempotencyKeyId = new IdempotencyKeyId(clientId, idempotencyKey);
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(idempotencyKeyId);

        if (existing.isPresent()) {
            IdempotencyKey saved = existing.get();

            // 3a. Same key, different body → client bug
            if (!saved.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyMismatchException("Key was already used with a different request body");
            }

            // 3b. Same key, same body, first request still running
            if (saved.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new DuplicateResourceException("Request is still being processed");
            }

            // 3c. Same key, same body, first request finished → replay its response
            return jsonMapper.readValue(saved.getResponseBody(), WorkOrderResponse.class);
        }

        // 4. New key → claim it (INSERT now; a parallel twin fails on the primary key → 409)
        Instant now = Instant.now();
        IdempotencyKey row = idempotencyKeyRepository.saveAndFlush(IdempotencyKey.builder()
                .clientId(clientId)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofHours(24)))
                .build());

        // 5. Do the real work
        WorkOrderResponse response = workOrderService.create(request);

        // 6. Save the response so retries can replay it
        row.setStatus(IdempotencyStatus.COMPLETED);
        row.setResponseStatus(201);
        row.setResponseBody(jsonMapper.writeValueAsString(response));

        return response;
    }

    private String hash(CreateWorkOrderRequest request) {
        try {
            String body = request.title() + "|" + request.description();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
