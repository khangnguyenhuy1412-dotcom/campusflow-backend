package com.campusflow.exchange.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param itemId        the item the requester wants
 * @param offeredItemId one of the requester's own items, required for EXCHANGE listings only
 */
public record CreateExchangeRequest(
        @NotNull Long itemId,
        Long offeredItemId,
        @Size(max = 1000) String message) {
}
