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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and checks the 6-digit codes emailed to users, for email verification and password reset.
 * Only the newest code of each purpose is valid; each code expires and allows a limited number of guesses.
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final VerificationProperties properties;
    private final ApplicationEventPublisher events;

    // ---------------------------------------------------------------- email verification

    @Transactional
    public void issueEmailVerificationCode(User user) {
        issueCode(user, VerificationPurpose.EMAIL_VERIFICATION);
    }

    /**
     * Sends a new verification code. Unknown or already verified emails are ignored silently,
     * so this endpoint cannot be used to check which emails are registered.
     */
    @Transactional
    public void resendEmailVerificationCode(String email) {
        findUser(email)
                .filter(user -> !user.isVerified())
                .ifPresent(user -> {
                    requireCooldownPassed(user, VerificationPurpose.EMAIL_VERIFICATION);
                    issueCode(user, VerificationPurpose.EMAIL_VERIFICATION);
                });
    }

    /** Marks the user's email as verified. Wrong guesses are counted even though an error is thrown. */
    @Transactional(noRollbackFor = ApiException.class)
    public User verifyEmail(String email, String code) {
        User user = findUser(email).orElseThrow(VerificationService::invalidCode);
        if (user.isVerified()) {
            throw ApiException.conflict("Email is already verified");
        }
        consumeCode(user, VerificationPurpose.EMAIL_VERIFICATION, code);
        user.setVerified(true);
        return user;
    }

    // ---------------------------------------------------------------- password reset

    /**
     * Emails a password reset code. Always behaves the same from the outside (no error for unknown
     * emails or during the cooldown), so it cannot be used to find out which emails are registered.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        findUser(email)
                .filter(User::isActive)
                .filter(user -> cooldownPassed(user, VerificationPurpose.PASSWORD_RESET))
                .ifPresent(user -> issueCode(user, VerificationPurpose.PASSWORD_RESET));
    }

    /** Checks and uses up a password reset code; returns the user whose password may be changed. */
    @Transactional(noRollbackFor = ApiException.class)
    public User consumePasswordResetCode(String email, String code) {
        User user = findUser(email).filter(User::isActive).orElseThrow(VerificationService::invalidCode);
        consumeCode(user, VerificationPurpose.PASSWORD_RESET, code);
        return user;
    }

    // ---------------------------------------------------------------- shared logic

    private void issueCode(User user, VerificationPurpose purpose) {
        codeRepository.deleteUnused(user.getId(), purpose);
        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        codeRepository.save(VerificationCode.builder()
                .user(user)
                .purpose(purpose)
                .codeHash(Hashing.sha256Hex(code))
                .expiresAt(Instant.now().plus(properties.codeTtl()))
                .build());
        events.publishEvent(new VerificationCodeIssuedEvent(
                user.getEmail(), user.getFullName(), code, purpose, properties.codeTtl()));
    }

    private void consumeCode(User user, VerificationPurpose purpose, String code) {
        VerificationCode verificationCode = codeRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByIdDesc(user.getId(), purpose)
                .orElseThrow(VerificationService::invalidCode);
        if (verificationCode.isExpired() || verificationCode.getAttempts() >= properties.maxAttempts()) {
            throw invalidCode();
        }
        if (!Hashing.matches(verificationCode.getCodeHash(), Hashing.sha256Hex(code))) {
            verificationCode.setAttempts(verificationCode.getAttempts() + 1);
            int remaining = properties.maxAttempts() - verificationCode.getAttempts();
            throw ApiException.badRequest(remaining > 0
                    ? "Incorrect verification code, " + remaining + " attempt(s) left"
                    : "Incorrect verification code. Please request a new one");
        }
        verificationCode.setUsedAt(Instant.now());
    }

    private boolean cooldownPassed(User user, VerificationPurpose purpose) {
        return codeRepository.findFirstByUserIdAndPurposeOrderByIdDesc(user.getId(), purpose)
                .map(last -> Duration.between(last.getCreatedAt(), Instant.now()).compareTo(properties.resendCooldown()) >= 0)
                .orElse(true);
    }

    private void requireCooldownPassed(User user, VerificationPurpose purpose) {
        codeRepository.findFirstByUserIdAndPurposeOrderByIdDesc(user.getId(), purpose).ifPresent(last -> {
            Duration sinceLast = Duration.between(last.getCreatedAt(), Instant.now());
            if (sinceLast.compareTo(properties.resendCooldown()) < 0) {
                long waitSeconds = properties.resendCooldown().minus(sinceLast).toSeconds() + 1;
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + waitSeconds + " seconds before requesting a new code");
            }
        });
    }

    private Optional<User> findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    private static ApiException invalidCode() {
        return ApiException.badRequest("Verification code is invalid or expired. Please request a new one");
    }
}
