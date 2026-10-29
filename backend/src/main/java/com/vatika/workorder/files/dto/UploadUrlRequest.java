package com.vatika.workorder.files.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UploadUrlRequest(
        @NotNull UUID workOrderId,
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 100) String contentType
) {
}