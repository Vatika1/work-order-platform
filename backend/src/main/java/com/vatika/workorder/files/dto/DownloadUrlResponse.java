package com.vatika.workorder.files.dto;

import java.time.Instant;

public record DownloadUrlResponse(
        String downloadUrl, Instant expiresAt
) {
}
