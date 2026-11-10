package com.campusflow.support;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Replaces the SMTP sender in integration tests: keeps every email in memory instead of sending it. */
public class RecordingMailSender extends JavaMailSenderImpl {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");
    private static final Duration WAIT = Duration.ofSeconds(10);

    private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(SimpleMailMessage... messages) {
        sent.addAll(List.of(messages));
    }

    @Override
    public void send(SimpleMailMessage message) {
        sent.add(message);
    }

    public List<SimpleMailMessage> sentTo(String email) {
        return sent.stream().filter(m -> m.getTo() != null && List.of(m.getTo()).contains(email)).toList();
    }

    /** Waits for an email (sent asynchronously) whose subject contains the text, newest first. */
    public SimpleMailMessage awaitEmail(String email, String subjectContains) {
        Instant deadline = Instant.now().plus(WAIT);
        while (Instant.now().isBefore(deadline)) {
            Optional<SimpleMailMessage> found = sentTo(email).reversed().stream()
                    .filter(m -> m.getSubject() != null && m.getSubject().contains(subjectContains))
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            sleep();
        }
        throw new AssertionError("No email to " + email + " with subject containing '" + subjectContains + "'");
    }

    public String awaitCode(String email, String subjectContains) {
        Matcher matcher = CODE.matcher(awaitEmail(email, subjectContains).getText());
        if (!matcher.find()) {
            throw new AssertionError("Email to " + email + " contains no 6-digit code");
        }
        return matcher.group(1);
    }

    private static void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
