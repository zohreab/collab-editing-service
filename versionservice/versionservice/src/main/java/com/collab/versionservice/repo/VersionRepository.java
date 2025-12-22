package com.collab.versionservice.repo;

import com.collab.versionservice.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VersionRepository extends JpaRepository<Version, Long> {
    List<Version> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    void deleteByDocumentId(java.util.UUID documentId);
}