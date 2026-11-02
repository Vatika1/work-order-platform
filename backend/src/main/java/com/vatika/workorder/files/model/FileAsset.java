package com.vatika.workorder.files.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "file_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAsset {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_allocated_version", nullable = false)
    private int lastAllocatedVersion;

    @OneToMany(mappedBy = "fileAsset", fetch = FetchType.LAZY)
    private List<FileVersion> versions = new ArrayList<>();
}