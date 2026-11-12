package com.campusflow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of the editable profile fields. Email, roles and university (detected from the
 * email domain) cannot be changed here.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]*", message = "must contain only letters and digits")
        String studentId,
        // Vietnamese mobile number: 0xxxxxxxxx or +84xxxxxxxxx
        @Pattern(regexp = "(0|\\+84)\\d{9}", message = "must be a valid Vietnamese phone number")
        String phoneNumber) {
}
