package com.collab.versionservice.controller;

import com.collab.versionservice.model.Version;
import com.collab.versionservice.repo.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    @Autowired
    private VersionRepository repository;

    @PostMapping
    public Version save(@RequestBody Version v) {
        if (v.getContent() == null) v.setContent(""); // Prevent null content
        v.setCreatedAt(LocalDateTime.now());
        return repository.save(v);
    }
    // Operation 2: Get History
    @GetMapping("/doc/{docId}")
    public List<Version> history(@PathVariable UUID docId) {
        return repository.findByDocumentIdOrderByCreatedAtDesc(docId);
    }

    // Operation 3: Get specific version (Revert)
    @GetMapping("/{id}")
    public Version getOne(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    // Operation 4: Cleanup history when a document is deleted
    @DeleteMapping("/doc/{docId}")
    @jakarta.transaction.Transactional
    public void deleteHistory(@PathVariable UUID docId) {
        repository.deleteByDocumentId(docId);
    }


}