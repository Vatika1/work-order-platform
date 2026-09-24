package com.vatika.workorder.workorder.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkOrderRequest(
        @NotBlank String title,
         String description
) {
}
