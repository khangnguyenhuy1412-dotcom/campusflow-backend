package com.campusflow.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase words separated by '-'")
        String slug,
        @Size(max = 1000) String description,
        // Icon name from the frontend icon set, e.g. "book-open"
        @Size(max = 50) @Pattern(regexp = "[a-z0-9-]*", message = "must be an icon name like 'book-open'")
        String iconUrl) {
}
