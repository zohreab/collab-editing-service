package com.collab.docservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDocRequest {
    @NotBlank
    public String title;

    public String content = "";
}
