package com.collab.docservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class VersionResponse {
    public Long id;
    public UUID documentId;
    public String authorUsername;
    public String content;
    public LocalDateTime createdAt;

    public VersionResponse() {}
}
