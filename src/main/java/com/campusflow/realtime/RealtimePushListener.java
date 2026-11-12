package com.campusflow.realtime;

import com.campusflow.chat.event.ChatMessageSentEvent;
import com.campusflow.notification.event.NotificationCreatedEvent;
import com.campusflow.support.event.SupportMessageSentEvent;
import com.campusflow.user.entity.Role;
import com.campusflow.user.repository.UserRepository;
import java.util.List;
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
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessage(ChatMessageSentEvent event) {
        for (Long participantId : event.participantIds()) {
            messagingTemplate.convertAndSendToUser(
                    participantId.toString(), WebSocketConfig.MESSAGES_QUEUE, event.message());
        }
    }

    /** The user sees the anonymous staff view; every moderator and admin gets the full staff view. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSupportMessage(SupportMessageSentEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.ticket().getUser().getId().toString(), WebSocketConfig.SUPPORT_QUEUE, event.userView());
        for (Long staffId : userRepository.findActiveIdsWithAnyRole(List.of(Role.MODERATOR, Role.ADMIN))) {
            messagingTemplate.convertAndSendToUser(staffId.toString(), WebSocketConfig.SUPPORT_QUEUE, event.staffView());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.recipientId().toString(), WebSocketConfig.NOTIFICATIONS_QUEUE, event.notification());
    }
}
