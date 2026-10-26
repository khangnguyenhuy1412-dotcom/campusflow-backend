package com.campusflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private final JwtConfig jwtConfig = new JwtConfig();

    private JwtService jwtService(JwtProperties properties) {
        SecretKey key = jwtConfig.jwtSecretKey(properties);
        return new JwtService(jwtConfig.jwtEncoder(key), properties);
    }

    private JwtDecoder jwtDecoder(JwtProperties properties) {
        return jwtConfig.jwtDecoder(jwtConfig.jwtSecretKey(properties), properties);
    }

    private static JwtProperties properties(String secret, String issuer, Duration accessTtl) {
        return new JwtProperties(secret, issuer, accessTtl, Duration.ofDays(7));
    }

    @Test
    void generatedTokenDecodesWithExpectedClaims() {
        JwtProperties props = properties(SECRET, "campusflow", Duration.ofMinutes(15));

        String token = jwtService(props).generateAccessToken(42L, "an@student.edu.vn", true, List.of("ROLE_STUDENT"));
        Jwt jwt = jwtDecoder(props).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_EMAIL)).isEqualTo("an@student.edu.vn");
        assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_ROLES)).containsExactly("ROLE_STUDENT");
        assertThat(jwt.getClaimAsBoolean(JwtService.CLAIM_EMAIL_VERIFIED)).isTrue();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties props = properties(SECRET, "campusflow", Duration.ofMinutes(15));
        JwtProperties otherProps = properties("another-secret-another-secret-another", "campusflow", Duration.ofMinutes(15));

        String token = jwtService(otherProps).generateAccessToken(1L, "a@b.vn", false, List.of("ROLE_STUDENT"));

        assertThatThrownBy(() -> jwtDecoder(props).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties props = properties(SECRET, "campusflow", Duration.ofMinutes(15));
        JwtProperties otherIssuer = properties(SECRET, "someone-else", Duration.ofMinutes(15));

        String token = jwtService(otherIssuer).generateAccessToken(1L, "a@b.vn", false, List.of("ROLE_STUDENT"));

        assertThatThrownBy(() -> jwtDecoder(props).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtProperties props = properties(SECRET, "campusflow", Duration.ofMinutes(15));
        // Default validator allows 60s clock skew, so expire well beyond that
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(20));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("campusflow")
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .build();
        String token = jwtConfig.jwtEncoder(jwtConfig.jwtSecretKey(props))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> jwtDecoder(props).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> properties("too-short", "campusflow", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }
}
