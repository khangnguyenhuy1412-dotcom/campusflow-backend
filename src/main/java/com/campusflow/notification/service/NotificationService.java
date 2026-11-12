package com.campusflow.notification.service;

import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.notification.dto.NotificationResponse;
import com.campusflow.notification.dto.UnreadCountResponse;
import com.campusflow.notification.entity.Notification;
import com.campusflow.notification.event.NotificationCreatedEvent;
import com.campusflow.notification.mail.NotificationEmailEvent;
import com.campusflow.notification.repository.NotificationRepository;
import com.campusflow.user.entity.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher events;

    /**
     * Stores the notification in the caller's transaction; after it commits the notification is
     * pushed live over WebSocket and, for important types, emailed. Users are never notified about
     * their own actions.
     */
    @Transactional
    public void create(NotificationDraft draft) {
        User recipient = draft.recipient();
        if (draft.actor() != null && draft.actor().getId().equals(recipient.getId())) {
            return;
        }
        Notification saved = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(draft.type())
                .title(draft.title())
                .message(draft.message())
                .actor(draft.actor())
                .item(draft.item())
                .exchangeRequest(draft.exchangeRequest())
                .supportTicket(draft.supportTicket())
                .build());
        events.publishEvent(new NotificationCreatedEvent(recipient.getId(), NotificationResponse.from(saved)));

        if (draft.type().isEmailed() && recipient.isEmailNotifications() && recipient.isActive()) {
            events.publishEvent(new NotificationEmailEvent(
                    recipient.getEmail(), recipient.getFullName(), draft.title(), draft.message()));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, Integer page, Integer size) {
        Pageable pageable = PageQuery.of(page, size, NEWEST_FIRST);
        return PageResponse.of(
                unreadOnly
                        ? notificationRepository.findByRecipientIdAndReadAtIsNull(userId, pageable)
                        : notificationRepository.findByRecipientId(userId, pageable),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByRecipientIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = getOwn(userId, notificationId);
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public UnreadCountResponse markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, Instant.now());
        return new UnreadCountResponse(0);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(getOwn(userId, notificationId));
    }

    /** Other users' notifications behave as if they do not exist. */
    private Notification getOwn(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
    }
}
