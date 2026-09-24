package com.vatika.workorder.workorder.service;

import com.vatika.workorder.shared.exception.NotFoundException;
import com.vatika.workorder.shared.security.JwtPrincipal;
import com.vatika.workorder.shared.tenancy.TenantContext;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;


    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request){
        UUID clientId = TenantContext.require();
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        UUID userId = principal.userId();

        Instant now = Instant.now();
        WorkOrder workOrder = WorkOrder.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .title(request.title())
                .description(request.description())
                .status(WorkOrderStatus.DRAFT)
                .createdBy(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);

        return WorkOrderResponse.from(savedWorkOrder);
    }

    public WorkOrderResponse getById(UUID id){
        UUID clientId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new NotFoundException("Work order not found"));

        return WorkOrderResponse.from(workOrder);
    }
}
