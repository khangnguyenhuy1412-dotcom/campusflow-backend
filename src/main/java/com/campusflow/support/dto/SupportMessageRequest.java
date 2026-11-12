package com.campusflow.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportMessageRequest(@NotBlank @Size(max = 2000) String content) {
}
