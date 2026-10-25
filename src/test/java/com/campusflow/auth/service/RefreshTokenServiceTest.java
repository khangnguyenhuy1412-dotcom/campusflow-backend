package com.campusflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.auth.entity.RefreshToken;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.common.exception.ApiException;
import com.campusflow.security.JwtProperties;
import com.campusflow.user.entity.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-test-secret-test-secret-123", "campusflow", Duration.ofMinutes(15), Duration.ofDays(7));
        service = new RefreshTokenService(repository, props);
        user = User.builder().id(7L).email("a@b.vn").active(true).build();
    }

    private RefreshToken stored(String raw, boolean revoked, Instant expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .user(user).tokenHash(RefreshTokenService.hash(raw)).revoked(revoked).expiresAt(expiresAt).build();
        when(repository.findByTokenHash(RefreshTokenService.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void createStoresOnlyTheHash() {
        String raw = service.create(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewOne() {
        RefreshToken old = stored("old-token", false, Instant.now().plusSeconds(3600));

        RefreshTokenService.Rotation rotation = service.rotate("old-token");

        assertThat(old.isRevoked()).isTrue();
        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.refreshToken()).isNotEqualTo("old-token");
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void reusingRevokedTokenRevokesAllSessions() {
        stored("stolen-token", true, Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> service.rotate("stolen-token")).isInstanceOf(ApiException.class);
        verify(repository).revokeAllByUserId(7L);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        stored("expired-token", false, Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.rotate("expired-token")).isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope")).isInstanceOf(ApiException.class);
    }
}
