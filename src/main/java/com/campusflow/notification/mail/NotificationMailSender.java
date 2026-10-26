package com.campusflow.notification.mail;

import com.campusflow.common.mail.AppMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails important notifications. Like verification emails, it runs only after the commit
 * (no email for an action that was rolled back) and in the background.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMailSender {

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationEmail(NotificationEmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject("CampusFlow - " + event.title());
        message.setText("""
                Xin chào %s,

                %s

                Mở CampusFlow để xem chi tiết.

                Bạn có thể tắt email thông báo trong phần Cài đặt tài khoản.
                CampusFlow Team
                """.formatted(event.fullName(), event.message()));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // The notification is still in the app, so a failed email is only logged
            log.error("Could not send notification email to {}", event.email(), e);
        }
    }
}
