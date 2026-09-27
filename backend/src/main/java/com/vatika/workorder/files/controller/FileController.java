package com.vatika.workorder.files.controller;

import com.vatika.workorder.files.dto.DownloadUrlResponse;
import com.vatika.workorder.files.dto.UploadUrlRequest;
import com.vatika.workorder.files.dto.UploadUrlResponse;
import com.vatika.workorder.files.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> createUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        UploadUrlResponse response = fileService.createUploadUrl(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmUpload(@PathVariable UUID id) {
        fileService.confirmUpload(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> createDownloadUrl(@PathVariable UUID id) {
        DownloadUrlResponse response = fileService.createDownloadUrl(id);
        return ResponseEntity.ok(response);

    }
}
