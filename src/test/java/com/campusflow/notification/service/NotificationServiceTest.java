package com.campusflow.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.common.exception.ApiException;
import com.campusflow.notification.entity.Notification;
import com.campusflow.notification.entity.NotificationType;
import com.campusflow.notification.mail.NotificationEmailEvent;
import com.campusflow.notification.repository.NotificationRepository;
import com.campusflow.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ApplicationEventPublisher events;
    @InjectMocks
    private NotificationService service;

    private final User owner = User.builder().id(1L).email("an@hcmus.edu.vn").fullName("An").build();
    private final User requester = User.builder().id(2L).email("binh@hcmus.edu.vn").fullName("Bình").build();

    private static NotificationDraft draft(User recipient, User actor, NotificationType type) {
        return new NotificationDraft(recipient, type, "Title", "Message", actor, null, null);
    }

    @Test
    void importantNotificationIsStoredAndEmailed() {
        service.create(draft(owner, requester, NotificationType.EXCHANGE_REQUESTED));

        verify(notificationRepository).save(any(Notification.class));
        verify(events).publishEvent(new NotificationEmailEvent("an@hcmus.edu.vn", "An", "Title", "Message"));
    }

    @Test
    void minorNotificationIsNotEmailed() {
        service.create(draft(owner, requester, NotificationType.REVIEW_RECEIVED));

        verify(notificationRepository).save(any(Notification.class));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void noEmailWhenUserTurnedItOff() {
        owner.setEmailNotifications(false);

        service.create(draft(owner, requester, NotificationType.EXCHANGE_ACCEPTED));

        verify(notificationRepository).save(any(Notification.class));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void usersAreNotNotifiedAboutTheirOwnActions() {
        service.create(draft(owner, owner, NotificationType.EXCHANGE_REQUESTED));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markReadOnlyWorksOnOwnNotifications() {
        Notification notification = Notification.builder().id(5L).recipient(owner).build();
        when(notificationRepository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.findByIdAndRecipientId(5L, 2L)).thenReturn(Optional.empty());

        service.markRead(1L, 5L);
        assertThat(notification.isRead()).isTrue();

        assertThatThrownBy(() -> service.markRead(2L, 5L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
