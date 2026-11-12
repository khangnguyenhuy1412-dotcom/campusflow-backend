package com.campusflow.exchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param code the 8-character code from the giver, or the full payload read from the QR code */
public record ConfirmHandoverRequest(@NotBlank @Size(max = 100) String code) {
}
