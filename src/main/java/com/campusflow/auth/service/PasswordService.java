package com.campusflow.auth.service;

import com.campusflow.auth.dto.ChangePasswordRequest;
import com.campusflow.auth.dto.ResetPasswordRequest;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.common.exception.ApiException;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import com.campusflow.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Change, forgot and reset password. Any password change logs out all other sessions. */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationService verificationService;

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("New password must be different from the current one");
        }
        setPassword(user, request.newPassword());
    }

    public void forgotPassword(String email) {
        verificationService.requestPasswordReset(email);
    }

    /**
     * The code was emailed to the user, so using it also proves they own the email address;
     * an unverified account becomes verified.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = verificationService.consumePasswordResetCode(request.email(), request.code());
        setPassword(user, request.newPassword());
        user.setVerified(true);
    }

    private void setPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        // Sign out everywhere: a stolen refresh token stops working after a password change
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }
}
