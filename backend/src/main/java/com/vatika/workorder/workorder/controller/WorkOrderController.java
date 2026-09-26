package com.vatika.workorder.workorder.controller;

import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.TransitionRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> login(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderResponse response = workOrderService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> getById(@PathVariable UUID id){
        WorkOrderResponse response = workOrderService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("{id}/transitions")
    public ResponseEntity<WorkOrderResponse> transition(@PathVariable UUID id,@Valid @RequestBody TransitionRequest request){
        WorkOrderResponse response = workOrderService.transition(id, request.targetStatus());
        return ResponseEntity.ok(response);
    }
}
