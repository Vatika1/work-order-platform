package com.vatika.workorder.workorder.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRequest(@NotNull UUID assigneeId) {
}
