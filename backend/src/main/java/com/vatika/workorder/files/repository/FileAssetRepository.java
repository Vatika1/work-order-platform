package com.vatika.workorder.files.repository;

import com.vatika.workorder.files.model.FileAsset;
import com.vatika.workorder.files.model.FileVersion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    Optional<FileAsset> findByIdAndClientId(UUID id, UUID clientId);
    List<FileAsset> findAllByClientIdAndWorkOrderId(UUID clientId, UUID workOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FileAsset f where f.id = :id and f.clientId = :clientId")
    Optional<FileAsset> findByIdAndClientIdForUpdate(@Param("id") UUID id,
                                                     @Param("clientId") UUID clientId);
}
