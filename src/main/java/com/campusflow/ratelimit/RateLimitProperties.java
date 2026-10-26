package com.campusflow.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param enabled switch to turn all limits off, e.g. for load tests; on by default */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled) {
}
