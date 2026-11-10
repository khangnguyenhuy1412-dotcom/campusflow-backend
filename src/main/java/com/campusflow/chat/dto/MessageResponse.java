package com.campusflow.chat.dto;

import com.campusflow.chat.entity.Message;
import java.time.Instant;

public record MessageResponse(Long id, Long conversationId, Long senderId, String content, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt());
    }
}
