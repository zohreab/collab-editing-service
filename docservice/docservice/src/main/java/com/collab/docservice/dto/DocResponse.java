package com.collab.docservice.dto;

import java.time.Instant;
import java.util.List; // Add this
import java.util.UUID;

public class DocResponse {
    public UUID id;
    public String title;
    public String content;
    public String ownerUsername;
    public List<String> collaborators; // 1. Added this field
    public Instant createdAt;
    public Instant updatedAt;

    // 2. Updated constructor to include collaborators
    public DocResponse(UUID id, String title, String content, String ownerUsername,
                       List<String> collaborators, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ownerUsername = ownerUsername;
        this.collaborators = collaborators;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}