package com.vatika.workorder.files.dto;

import java.util.List;
import java.util.UUID;

public record FileWithVersionsResponse(
        UUID id,
        String fileName,
        String contentType,
        List<FileVersionResponse> versions) {}
