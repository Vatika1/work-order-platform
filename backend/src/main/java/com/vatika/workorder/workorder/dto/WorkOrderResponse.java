package com.vatika.workorder.workorder.dto;

import com.vatika.workorder.workorder.model.WorkOrder;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderResponse (
        UUID id,
        UUID clientId,
        String title,
        String description,
        com.vatika.workorder.workorder.model.WorkOrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkOrderResponse from(WorkOrder wo) {
        return new WorkOrderResponse(
                wo.getId(), wo.getClientId(), wo.getTitle(), wo.getDescription(),
                wo.getStatus(), wo.getCreatedAt(), wo.getUpdatedAt());
    }
}
