package com.vatika.workorder.shared.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse (
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<String> details
){
    public static ErrorResponse of(int status, String error, String message, String path) {
        String correlationId = org.slf4j.MDC.get("correlationId");
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, List.of());
    }
}