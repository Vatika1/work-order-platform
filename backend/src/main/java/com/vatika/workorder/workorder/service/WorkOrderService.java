package com.vatika.workorder.workorder.service;

import com.vatika.workorder.shared.dto.PageResponse;
import com.vatika.workorder.shared.exception.BadRequestException;
import com.vatika.workorder.shared.exception.NotFoundException;
import com.vatika.workorder.shared.security.JwtPrincipal;
import com.vatika.workorder.shared.tenancy.TenantContext;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.model.WorkOrderTransitions;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "title", "status");
    private static final int MAX_PAGE_SIZE = 100;


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

    @Transactional
    public WorkOrderResponse claimNext(){
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        UUID userId = principal.userId();
        WorkOrder workOrder =workOrderRepository.findNextUnassignedForUpdate()
                .orElseThrow(() -> new NotFoundException("Work order not found"));
        workOrder.setAssigneeId(userId);
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        return WorkOrderResponse.from(savedWorkOrder);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, WorkOrderStatus target){
        UUID clientId = TenantContext.require();
        WorkOrder workOrder =workOrderRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new NotFoundException("Work order not found"));

        WorkOrderTransitions.validateTransition(workOrder.getStatus(), target);
        workOrder.setStatus(target);
        workOrder.setUpdatedAt(Instant.now());
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);

        return WorkOrderResponse.from(savedWorkOrder);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> list(WorkOrderStatus status, Pageable pageable) {
        UUID clientId = TenantContext.require();
        validatePageable(pageable);

        Page<WorkOrder> page = (status == null)
                ? workOrderRepository.findAllByClientId(clientId, pageable)
                : workOrderRepository.findAllByClientIdAndStatus(clientId, status, pageable);

        return PageResponse.from(page.map(WorkOrderResponse::from));
    }

    private void validatePageable(Pageable pageable){
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be at most " + MAX_PAGE_SIZE);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by: " + order.getProperty());
            }
        }
    }

}
