package com.campusflow.chat.dto;

import com.campusflow.chat.entity.Conversation;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

/**
 * A conversation as seen by one participant.
 *
 * @param counterpart the other person (seller for the buyer, buyer for the seller)
 * @param buyer       whether the viewer is the buyer
 */
public record ConversationResponse(
        Long id,
        ChatItemResponse item,
        UserSummaryResponse counterpart,
        boolean buyer,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount,
        Instant createdAt) {

    public static ConversationResponse from(Conversation conversation, Long viewerId, long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                ChatItemResponse.from(conversation.getItem()),
                UserSummaryResponse.from(conversation.counterpartOf(viewerId)),
                conversation.getBuyer().getId().equals(viewerId),
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(),
                unreadCount,
                conversation.getCreatedAt());
    }
}
