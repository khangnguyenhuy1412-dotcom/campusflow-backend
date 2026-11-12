package com.campusflow.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param emailDomain lowercase domain such as "hcmus.edu.vn"; subdomains match automatically */
public record UniversityRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 30) String shortName,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "([a-z0-9-]+\\.)+[a-z]{2,}", message = "must be a lowercase domain like 'hcmus.edu.vn'")
        String emailDomain) {
}
