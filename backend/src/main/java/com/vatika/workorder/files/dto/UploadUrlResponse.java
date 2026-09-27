package com.vatika.workorder.files.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadUrlResponse(
        UUID fileAssetId, String uploadUrl, Instant expiresAt
) {
}
