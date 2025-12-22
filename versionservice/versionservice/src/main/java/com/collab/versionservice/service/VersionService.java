package com.collab.versionservice.service;

import com.collab.versionservice.model.Version;
import com.collab.versionservice.repo.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID; // Added UUID import

@Service
public class VersionService {

    @Autowired
    private VersionRepository repository;

    /**
     * Operation 1: Maintain History
     * Saves a full snapshot of the document content.
     */
    public Version saveSnapshot(Version version) {
        version.setCreatedAt(LocalDateTime.now());
        return repository.save(version);
    }

    /**
     * Operation 2: Get History (Tracking contributions)
     * Retrieves all versions linked to a specific Document UUID.
     */
    public List<Version> getHistory(UUID docId) { // Changed from Long to UUID
        return repository.findByDocumentIdOrderByCreatedAtDesc(docId);
    }

    /**
     * Operation 3: Revert to previous version
     * Fetches a specific version by its primary key (Long ID).
     */
    public Version getVersion(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Version history not found for ID: " + id));
    }
}