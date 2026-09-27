package com.vatika.workorder.files.service;

import com.vatika.workorder.files.dto.DownloadUrlResponse;
import com.vatika.workorder.files.dto.UploadUrlRequest;
import com.vatika.workorder.files.dto.UploadUrlResponse;
import com.vatika.workorder.files.model.FileAsset;
import com.vatika.workorder.files.model.FileVersion;
import com.vatika.workorder.files.repository.FileAssetRepository;
import com.vatika.workorder.files.repository.FileVersionRepository;
import com.vatika.workorder.shared.exception.NotFoundException;
import com.vatika.workorder.shared.exception.UploadNotCompletedException;
import com.vatika.workorder.shared.security.JwtPrincipal;
import com.vatika.workorder.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class FileService {

    private final FileAssetRepository fileAssetRepository;
    private final FileVersionRepository fileVersionRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    private String buildKey(UUID clientId, UUID fileAssetId, int version) {
        return "clients/" + clientId + "/" + fileAssetId + "/v" + version;
    }

    @Transactional
    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        UUID clientId = TenantContext.require();
        UUID fileAssetId = UUID.randomUUID();
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        UUID userId = principal.userId();
        Instant now = Instant.now();

        String key = buildKey(clientId, fileAssetId, 1);

        FileAsset fileAsset = FileAsset.builder()
                .id(fileAssetId)
                .clientId(clientId)
                .workOrderId(request.workOrderId())
                .fileName(request.fileName())
                .contentType(request.contentType())
                .uploadedBy(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        fileAssetRepository.save(fileAsset);

        //request to upload this file "key" in S3 bucket
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        //Request to generate a pre-signed upload URL. Take this upload objectRequest and make me a temporary signed URL for it,
        // valid for this long.
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        //URL gets generated
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new UploadUrlResponse(fileAssetId, presigned.url().toString(),
                presigned.expiration());
    }

    @Transactional
    public void confirmUpload(UUID fileAssetId) {
        UUID clientId = TenantContext.require();
        fileAssetRepository.findByIdAndClientId(fileAssetId, clientId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        int nextVersion = fileVersionRepository.findTopByFileAssetIdOrderByVersionNumberDesc(fileAssetId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        String key = buildKey(clientId, fileAssetId, nextVersion);
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new UploadNotCompletedException(fileAssetId);
        }

        FileVersion fileVersion = FileVersion.builder()
                .id(UUID.randomUUID())
                .fileAssetId(fileAssetId)
                .s3Key(key)
                .versionNumber(nextVersion)
                .createdAt(Instant.now())
                .sizeBytes(head.contentLength())
                .build();
        fileVersionRepository.save(fileVersion);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse createDownloadUrl(UUID fileAssetId) {
        UUID clientId = TenantContext.require();
        fileAssetRepository.findByIdAndClientId(fileAssetId, clientId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        FileVersion version = fileVersionRepository.findTopByFileAssetIdOrderByVersionNumberDesc(fileAssetId)
                .orElseThrow(() -> new NotFoundException("No file uploaded"));

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(version.getS3Key())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

        return new DownloadUrlResponse(presigned.url().toString(),
                presigned.expiration());
    }
}
