package com.vatika.workorder.workorder.service;

import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.shared.exception.BadRequestException;
import com.vatika.workorder.shared.exception.NotFoundException;
import com.vatika.workorder.shared.security.JwtPrincipal;
import com.vatika.workorder.workorder.dto.AssignRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StaffWorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

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
    public WorkOrderResponse assign(AssignRequest request, UUID workOrderId){
        WorkOrder workOrder =workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException("Work order not found"));

        User user = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new NotFoundException("User not found"));
         if(user.getRole() != Role.STAFF){
             throw new BadRequestException("Assignee must be a staff user");
         }

         workOrder.setAssigneeId(user.getId());
         WorkOrder saved = workOrderRepository.save(workOrder);
         return WorkOrderResponse.from(saved);
    }
}
