package com.campusflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 150) String email,
        // BCrypt only uses the first 72 bytes
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 20) String studentId,
        @Size(max = 150) String university) {
}
