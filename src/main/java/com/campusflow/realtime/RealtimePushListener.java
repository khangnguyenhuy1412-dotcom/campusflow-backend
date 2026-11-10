package com.campusflow.realtime;

import com.campusflow.chat.event.ChatMessageSentEvent;
import com.campusflow.notification.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes events to connected clients, only after the data is committed: a client never receives
 * a message that a reload would not show. Users that are not connected simply miss the push and
 * see the data next time they load it.
 */
@Component
@RequiredArgsConstructor
public class RealtimePushListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessage(ChatMessageSentEvent event) {
        for (Long participantId : event.participantIds()) {
            messagingTemplate.convertAndSendToUser(
                    participantId.toString(), WebSocketConfig.MESSAGES_QUEUE, event.message());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.recipientId().toString(), WebSocketConfig.NOTIFICATIONS_QUEUE, event.notification());
    }
}
