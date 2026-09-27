package com.vatika.workorder.files.repository;

import com.vatika.workorder.files.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileVersionRepository extends JpaRepository<FileVersion, UUID> {
    Optional<FileVersion> findTopByFileAssetIdOrderByVersionNumberDesc(UUID fileAssetId);
}
