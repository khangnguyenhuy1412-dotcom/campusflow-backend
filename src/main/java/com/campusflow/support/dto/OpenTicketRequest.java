package com.campusflow.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param relatedReportId optional: a report you filed or that concerns you (e.g. about your review) */
public record OpenTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 2000) String message,
        Long relatedReportId) {
}
