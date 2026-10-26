package com.campusflow.verification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(Duration codeTtl, int maxAttempts, Duration resendCooldown) {
}
