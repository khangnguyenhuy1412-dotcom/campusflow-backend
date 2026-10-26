package com.campusflow.verification.mail;

import com.campusflow.verification.entity.VerificationPurpose;
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
 * Emails verification codes. Runs only after the code is committed to the database,
 * and in the background so a slow mail server does not slow down the API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCodeIssued(VerificationCodeIssuedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject(subject(event.purpose()));
        message.setText(body(event));
        try {
            mailSender.send(message);
            log.info("Sent {} code to {}", event.purpose(), event.email());
        } catch (MailException e) {
            // The user can ask for a new code, so log instead of failing
            log.error("Could not send {} code to {}", event.purpose(), event.email(), e);
        }
    }

    private static String subject(VerificationPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "CampusFlow - Mã xác thực email sinh viên";
            case PASSWORD_RESET -> "CampusFlow - Mã đặt lại mật khẩu";
        };
    }

    private static String body(VerificationCodeIssuedEvent event) {
        String action = switch (event.purpose()) {
            case EMAIL_VERIFICATION -> "xác thực email sinh viên của bạn";
            case PASSWORD_RESET -> "đặt lại mật khẩu";
        };
        return """
                Xin chào %s,

                Mã để %s trên CampusFlow là:

                    %s

                Mã có hiệu lực trong %d phút. Không chia sẻ mã này với bất kỳ ai.
                Nếu bạn không yêu cầu mã này, hãy bỏ qua email.

                CampusFlow Team
                """.formatted(event.fullName(), action, event.code(), event.validFor().toMinutes());
    }
}
