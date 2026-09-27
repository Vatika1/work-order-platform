package com.vatika.workorder.files.repository;

import com.vatika.workorder.files.model.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    Optional<FileAsset> findByIdAndClientId(UUID id, UUID clientId);
    List<FileAsset> findAllByClientIdAndWorkOrderId(UUID clientId, UUID workOrderId);
}
