package com.vatika.workorder.files.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadUrlRequest(
        @NotNull UUID workOrderId, @NotBlank String fileName, @NotBlank String contentType
) {
}
