package com.campusflow.chat.event;

import com.campusflow.chat.dto.MessageResponse;
import java.util.List;

/**
 * Published when a message is saved; delivered over WebSocket after the transaction commits.
 *
 * @param participantIds both participants, so the sender's other open devices update as well
 */
public record ChatMessageSentEvent(MessageResponse message, List<Long> participantIds) {
}
