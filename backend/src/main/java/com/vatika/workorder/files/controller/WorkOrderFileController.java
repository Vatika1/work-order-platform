package com.vatika.workorder.files.controller;

import com.vatika.workorder.files.dto.FileWithVersionsResponse;
import com.vatika.workorder.files.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class WorkOrderFileController {

    private final FileService fileService;

    @GetMapping("/work-orders/{workOrderId}/files")
    public ResponseEntity<List<FileWithVersionsResponse>> listFilesForWorkOrder(@PathVariable UUID workOrderId) {
        List<FileWithVersionsResponse> response = fileService.listFilesForWorkOrder(workOrderId);
        return ResponseEntity.ok(response);

    }

}
