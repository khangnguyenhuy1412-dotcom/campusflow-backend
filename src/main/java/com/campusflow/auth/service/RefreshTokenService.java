package com.campusflow.auth.service;

import com.campusflow.auth.entity.RefreshToken;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.common.exception.ApiException;
import com.campusflow.common.util.Hashing;
import com.campusflow.security.JwtProperties;
import com.campusflow.user.entity.User;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are opaque random strings. Only their SHA-256 hash is stored, and each
 * one can be used once: refreshing revokes it and issues a new one (rotation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public record Rotation(User user, String refreshToken) {
    }

    /** Creates a refresh token for the user and returns the raw value to send to the client. */
    @Transactional
    public String create(User user) {
        String rawToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()))
                .build());
        return rawToken;
    }

    /**
     * Revokes the given token and issues a new one. Re-using an already revoked token
     * means it was probably stolen, so every session of that user is revoked.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalidToken);
        User user = token.getUser();

        if (token.isRevoked()) {
            log.warn("Revoked refresh token reused for user {}; revoking all sessions", user.getId());
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw invalidToken();
        }
        if (token.isExpired() || !user.isActive()) {
            throw invalidToken();
        }

        token.setRevoked(true);
        return new Rotation(user, create(user));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.setRevoked(true));
    }

    private static ApiException invalidToken() {
        return ApiException.unauthorized("Invalid or expired refresh token");
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        return Hashing.sha256Hex(rawToken);
    }
}
