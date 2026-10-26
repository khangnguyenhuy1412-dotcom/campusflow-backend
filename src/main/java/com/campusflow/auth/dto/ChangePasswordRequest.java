package com.campusflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        // BCrypt only uses the first 72 bytes
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
