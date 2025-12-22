package com.collab.docservice.dto;

import java.util.UUID;

public class VersionSnapshotRequest {
    public UUID documentId;
    public String authorUsername;
    public String content;

    public VersionSnapshotRequest() {}

    public VersionSnapshotRequest(UUID documentId, String authorUsername, String content) {
        this.documentId = documentId;
        this.authorUsername = authorUsername;
        this.content = content;
    }
}
