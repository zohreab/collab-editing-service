package com.collab.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangePasswordRequest {
    @NotBlank
    public String currentPassword;

    @NotBlank
    public String newPassword;
}
