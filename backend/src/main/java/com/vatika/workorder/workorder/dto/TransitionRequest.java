package com.vatika.workorder.workorder.dto;

import com.vatika.workorder.workorder.model.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull WorkOrderStatus targetStatus) {
}
