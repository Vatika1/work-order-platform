package com.vatika.workorder.workorder.controller;

import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.service.StaffWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/staff/work-orders")
public class StaffWorkOrderController {

    private final StaffWorkOrderService staffWorkOrderService;

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/claim-next")
    public ResponseEntity<WorkOrderResponse> claimNext(){
        WorkOrderResponse response = staffWorkOrderService.claimNext();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
