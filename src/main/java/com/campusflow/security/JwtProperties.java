package com.campusflow.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a 256-bit key

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be set and at least " + MIN_SECRET_BYTES + " bytes long");
        }
    }
}
