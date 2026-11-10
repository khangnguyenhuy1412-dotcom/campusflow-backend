package com.campusflow.chat.dto;

import jakarta.validation.constraints.NotNull;

/** @param itemId the item to ask its owner about */
public record StartConversationRequest(@NotNull Long itemId) {
}
