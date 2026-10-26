package com.campusflow.ratelimit;

import io.github.bucket4j.Bucket;
import java.time.Duration;

/**
 * Limits per client (IP address or user, depending on where the policy is applied).
 * Each policy is a token bucket: {@code capacity} requests, refilled gradually over {@code period}.
 */
public enum RateLimitPolicy {

    /** Baseline for every /api request, per IP: stops simple request floods. */
    API(300, Duration.ofMinutes(1)),

    /** Login attempts per IP. */
    LOGIN(20, Duration.ofMinutes(15)),
    /**
     * Login attempts per target email, so password guessing is limited even when spread over many IPs.
     * Trade-off: someone can block logins to an account for up to 15 minutes; acceptable for this app.
     */
    LOGIN_EMAIL(10, Duration.ofMinutes(15)),
    REGISTER(10, Duration.ofHours(1)),
    REFRESH_TOKEN(60, Duration.ofMinutes(15)),
    /** Endpoints that check a 6-digit code (verify email, reset password). */
    VERIFY_CODE(20, Duration.ofMinutes(15)),
    /** Endpoints that send an email (resend verification, forgot password). */
    SEND_EMAIL(10, Duration.ofMinutes(15)),
    /** Change password checks the current password, so it is guessable like a login. */
    PASSWORD_CHANGE(5, Duration.ofMinutes(15)),

    /** Image uploads per user. */
    UPLOAD(30, Duration.ofMinutes(10)),
    /** New items, exchange requests and reviews per user (shared budget against spam). */
    CREATE_CONTENT(30, Duration.ofHours(1));

    private final long capacity;
    private final Duration period;

    RateLimitPolicy(long capacity, Duration period) {
        this.capacity = capacity;
        this.period = period;
    }

    public long capacity() {
        return capacity;
    }

    public Duration period() {
        return period;
    }

    Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, period))
                .build();
    }
}
