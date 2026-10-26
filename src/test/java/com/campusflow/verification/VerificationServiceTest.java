package com.campusflow.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.common.exception.ApiException;
import com.campusflow.common.util.Hashing;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import com.campusflow.verification.entity.VerificationCode;
import com.campusflow.verification.entity.VerificationPurpose;
import com.campusflow.verification.mail.VerificationCodeIssuedEvent;
import com.campusflow.verification.repository.VerificationCodeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final VerificationPurpose EMAIL = VerificationPurpose.EMAIL_VERIFICATION;

    @Mock
    private VerificationCodeRepository codeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher events;

    private VerificationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new VerificationService(codeRepository, userRepository,
                new VerificationProperties(Duration.ofMinutes(10), 5, Duration.ofSeconds(60)), events);
        user = User.builder().id(1L).email("an@student.hcmus.edu.vn").fullName("An").build();
    }

    private VerificationCode storedCode(String code, int attempts, Instant expiresAt) {
        VerificationCode stored = VerificationCode.builder()
                .user(user).purpose(EMAIL).codeHash(Hashing.sha256Hex(code))
                .attempts(attempts).expiresAt(expiresAt).createdAt(Instant.now()).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByIdDesc(1L, EMAIL))
                .thenReturn(Optional.of(stored));
        return stored;
    }

    @Test
    void issueStoresHashAndEmailsTheCode() {
        service.issueEmailVerificationCode(user);

        ArgumentCaptor<VerificationCode> saved = ArgumentCaptor.forClass(VerificationCode.class);
        ArgumentCaptor<VerificationCodeIssuedEvent> event = ArgumentCaptor.forClass(VerificationCodeIssuedEvent.class);
        verify(codeRepository).deleteUnused(1L, EMAIL);
        verify(codeRepository).save(saved.capture());
        verify(events).publishEvent(event.capture());

        assertThat(event.getValue().code()).matches("\\d{6}");
        assertThat(saved.getValue().getCodeHash()).isEqualTo(Hashing.sha256Hex(event.getValue().code()));
    }

    @Test
    void correctCodeVerifiesUser() {
        VerificationCode stored = storedCode("123456", 0, Instant.now().plusSeconds(600));

        User result = service.verifyEmail(" AN@student.hcmus.edu.vn ", "123456");

        assertThat(result.isVerified()).isTrue();
        assertThat(stored.getUsedAt()).isNotNull();
    }

    @Test
    void wrongCodeCountsAttempt() {
        VerificationCode stored = storedCode("123456", 0, Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> service.verifyEmail(user.getEmail(), "000000"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("4 attempt(s) left");
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(user.isVerified()).isFalse();
    }

    @Test
    void codeIsLockedAfterMaxAttempts() {
        storedCode("123456", 5, Instant.now().plusSeconds(600));

        // Even the right code is refused once the attempts are used up
        assertThatThrownBy(() -> service.verifyEmail(user.getEmail(), "123456"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("invalid or expired");
        assertThat(user.isVerified()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() {
        storedCode("123456", 0, Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.verifyEmail(user.getEmail(), "123456"))
                .hasMessageContaining("invalid or expired");
    }

    @Test
    void resendWithinCooldownIsRejected() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeOrderByIdDesc(1L, EMAIL)).thenReturn(Optional.of(
                VerificationCode.builder().createdAt(Instant.now().minusSeconds(10)).build()));

        assertThatThrownBy(() -> service.resendEmailVerificationCode(user.getEmail()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(codeRepository, never()).save(any());
    }

    @Test
    void resendForUnknownEmailDoesNothing() {
        when(userRepository.findByEmail("ghost@x.edu.vn")).thenReturn(Optional.empty());

        service.resendEmailVerificationCode("ghost@x.edu.vn");

        verify(codeRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }
}
