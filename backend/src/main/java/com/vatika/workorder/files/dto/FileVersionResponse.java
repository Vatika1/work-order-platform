package com.vatika.workorder.files.dto;

import java.time.Instant;

public record FileVersionResponse(int versionNumber, long sizeBytes, Instant uploadedAt) {}

