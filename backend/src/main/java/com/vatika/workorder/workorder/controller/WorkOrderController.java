package com.vatika.workorder.workorder.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vatika.workorder.shared.dto.PageResponse;
import com.vatika.workorder.shared.idempotency.IdempotencyService;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.TransitionRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateWorkOrderRequest request) throws JsonProcessingException {

        WorkOrderResponse response = (idempotencyKey == null)
                ? workOrderService.create(request)
                : idempotencyService.create(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> getById(@PathVariable UUID id){
        WorkOrderResponse response = workOrderService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transitions")
    public ResponseEntity<WorkOrderResponse> transition(@PathVariable UUID id,@Valid @RequestBody TransitionRequest request){
        WorkOrderResponse response = workOrderService.transition(id, request.targetStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkOrderResponse>> list(
            @RequestParam(required = false) WorkOrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workOrderService.list(status, pageable));
    }
}
