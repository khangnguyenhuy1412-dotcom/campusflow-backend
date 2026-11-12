package com.campusflow.exchange.dto;

import java.time.Instant;

/**
 * Shown by the giver at the meetup. The receiver scans {@code qrImage} (it encodes
 * {@code qrPayload}) or types {@code code}. Only a hash is stored, so the code cannot be shown again.
 */
public record HandoverCodeResponse(String code, String qrPayload, String qrImage, Instant expiresAt) {
}
