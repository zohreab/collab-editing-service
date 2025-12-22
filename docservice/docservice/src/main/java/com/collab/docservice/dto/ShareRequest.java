package com.collab.docservice.dto;

import jakarta.validation.constraints.NotBlank;

public class ShareRequest {
    @NotBlank
    public String collaboratorUsername;
}