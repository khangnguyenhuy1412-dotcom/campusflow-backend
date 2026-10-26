package com.campusflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.auth.dto.ChangePasswordRequest;
import com.campusflow.auth.dto.ResetPasswordRequest;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import com.campusflow.verification.VerificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationService verificationService;

    // Low cost factor keeps the test fast
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private PasswordService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordService(userRepository, passwordEncoder, refreshTokenRepository, verificationService);
        user = User.builder().id(1L).email("an@hcmus.edu.vn").password(passwordEncoder.encode("old-password")).build();
    }

    @Test
    void changePasswordRevokesAllSessions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.changePassword(1L, new ChangePasswordRequest("old-password", "new-password"));

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void changePasswordNeedsCorrectCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(1L, new ChangePasswordRequest("wrong", "new-password")))
                .hasMessageContaining("Current password is incorrect");
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void newPasswordMustDiffer() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(1L, new ChangePasswordRequest("old-password", "old-password")))
                .hasMessageContaining("different");
    }

    @Test
    void resetPasswordSetsPasswordVerifiesEmailAndRevokesSessions() {
        when(verificationService.consumePasswordResetCode("an@hcmus.edu.vn", "123456")).thenReturn(user);

        service.resetPassword(new ResetPasswordRequest("an@hcmus.edu.vn", "123456", "brand-new-pass"));

        assertThat(passwordEncoder.matches("brand-new-pass", user.getPassword())).isTrue();
        assertThat(user.isVerified()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }
}
