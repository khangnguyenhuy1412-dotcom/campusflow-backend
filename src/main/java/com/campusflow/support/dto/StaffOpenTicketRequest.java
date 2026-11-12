package com.campusflow.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Staff contact a user, e.g. to ask both sides of a disputed review for their version. */
public record StaffOpenTicketRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 2000) String message,
        Long relatedReportId) {
}
