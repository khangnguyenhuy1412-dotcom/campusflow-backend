package com.campusflow.auth.controller;

import com.campusflow.auth.dto.AuthResponse;
import com.campusflow.auth.dto.ForgotPasswordRequest;
import com.campusflow.auth.dto.LoginRequest;
import com.campusflow.auth.dto.RefreshTokenRequest;
import com.campusflow.auth.dto.RegisterRequest;
import com.campusflow.auth.dto.ResendVerificationRequest;
import com.campusflow.auth.dto.ResetPasswordRequest;
import com.campusflow.auth.dto.VerifyEmailRequest;
import com.campusflow.auth.service.AuthService;
import com.campusflow.auth.service.PasswordService;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import com.campusflow.ratelimit.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Register, login, tokens, email verification and password reset")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Register with a student email (*.edu.vn); a verification code is emailed")
    @RateLimit(policy = RateLimitPolicy.REGISTER, key = RateLimit.Key.IP)
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Log in with email and password; returns access + refresh token")
    @RateLimit(policy = RateLimitPolicy.LOGIN, key = RateLimit.Key.IP)
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Per IP is enforced by @RateLimit; per email stops guessing spread across many IPs
        rateLimiter.consume(RateLimitPolicy.LOGIN_EMAIL, request.email().trim().toLowerCase(Locale.ROOT));
        return authService.login(request);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
    @RateLimit(policy = RateLimitPolicy.REFRESH_TOKEN, key = RateLimit.Key.IP)
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Verify the student email with the 6-digit code")
    @RateLimit(policy = RateLimitPolicy.VERIFY_CODE, key = RateLimit.Key.IP)
    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @Operation(summary = "Send a new verification code (60s cooldown)")
    @RateLimit(policy = RateLimitPolicy.SEND_EMAIL, key = RateLimit.Key.IP)
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationCode(request.email());
    }

    /** Always 202, whether or not the email exists, so it cannot be used to find registered emails. */
    @Operation(summary = "Email a password reset code; always 202")
    @RateLimit(policy = RateLimitPolicy.SEND_EMAIL, key = RateLimit.Key.IP)
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request.email());
    }

    @Operation(summary = "Set a new password with the emailed code; logs out all sessions")
    @RateLimit(policy = RateLimitPolicy.VERIFY_CODE, key = RateLimit.Key.IP)
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
    }

    @Operation(summary = "Revoke a refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
