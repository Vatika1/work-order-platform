package com.vatika.workorder.files.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    @Id
    private UUID id;

    @Column(name = "file_asset_id", nullable = false)
    private UUID fileAssetId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}