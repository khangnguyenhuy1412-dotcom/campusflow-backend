package com.campusflow.verification;

import com.campusflow.common.exception.ApiException;
import com.campusflow.common.util.Hashing;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import com.campusflow.verification.entity.VerificationCode;
import com.campusflow.verification.entity.VerificationPurpose;
import com.campusflow.verification.mail.VerificationCodeIssuedEvent;
import com.campusflow.verification.repository.VerificationCodeRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and checks the 6-digit codes emailed to students. Only the newest code is valid,
 * each code expires and allows a limited number of wrong guesses.
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final VerificationPurpose EMAIL = VerificationPurpose.EMAIL_VERIFICATION;

    private final VerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final VerificationProperties properties;
    private final ApplicationEventPublisher events;

    @Transactional
    public void issueEmailVerificationCode(User user) {
        codeRepository.deleteUnused(user.getId(), EMAIL);
        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        codeRepository.save(VerificationCode.builder()
                .user(user)
                .purpose(EMAIL)
                .codeHash(Hashing.sha256Hex(code))
                .expiresAt(Instant.now().plus(properties.codeTtl()))
                .build());
        events.publishEvent(new VerificationCodeIssuedEvent(
                user.getEmail(), user.getFullName(), code, EMAIL, properties.codeTtl()));
    }

    /**
     * Sends a new code. Unknown or already verified emails are ignored silently,
     * so this endpoint cannot be used to check which emails are registered.
     */
    @Transactional
    public void resendEmailVerificationCode(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null || user.isVerified()) {
            return;
        }
        codeRepository.findFirstByUserIdAndPurposeOrderByIdDesc(user.getId(), EMAIL).ifPresent(last -> {
            Duration sinceLast = Duration.between(last.getCreatedAt(), Instant.now());
            if (sinceLast.compareTo(properties.resendCooldown()) < 0) {
                long waitSeconds = properties.resendCooldown().minus(sinceLast).toSeconds() + 1;
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + waitSeconds + " seconds before requesting a new code");
            }
        });
        issueEmailVerificationCode(user);
    }

    /** Marks the user's email as verified. Wrong guesses are counted even though an error is thrown. */
    @Transactional(noRollbackFor = ApiException.class)
    public User verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElseThrow(VerificationService::invalidCode);
        if (user.isVerified()) {
            throw ApiException.conflict("Email is already verified");
        }
        VerificationCode verificationCode = codeRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByIdDesc(user.getId(), EMAIL)
                .orElseThrow(VerificationService::invalidCode);
        if (verificationCode.isExpired() || verificationCode.getAttempts() >= properties.maxAttempts()) {
            throw invalidCode();
        }
        if (!Hashing.matches(verificationCode.getCodeHash(), Hashing.sha256Hex(code))) {
            verificationCode.setAttempts(verificationCode.getAttempts() + 1);
            int remaining = properties.maxAttempts() - verificationCode.getAttempts();
            throw new ApiException(HttpStatus.BAD_REQUEST, remaining > 0
                    ? "Incorrect verification code, " + remaining + " attempt(s) left"
                    : "Incorrect verification code. Please request a new one");
        }

        verificationCode.setUsedAt(Instant.now());
        user.setVerified(true);
        return user;
    }

    private static ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired. Please request a new one");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
