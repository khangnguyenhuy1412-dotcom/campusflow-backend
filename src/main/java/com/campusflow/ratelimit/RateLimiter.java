package com.campusflow.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * In-memory token buckets, one per (policy, client key). The cache is bounded so many distinct
 * IPs cannot exhaust memory, and entries live longer than the longest policy period so evicting
 * an idle bucket never hands out a fresh budget early.
 * <p>
 * Limits are per application instance. When the app runs on several servers, swap this
 * for a shared store (e.g. bucket4j with Redis).
 */
@Component
public class RateLimiter {

    private static final long MAX_TRACKED_CLIENTS = 100_000;

    private final boolean enabled;
    private final Cache<String, Bucket> buckets;

    public RateLimiter(RateLimitProperties properties) {
        this.enabled = properties.enabled();
        Duration longestPeriod = Arrays.stream(RateLimitPolicy.values())
                .map(RateLimitPolicy::period)
                .max(Duration::compareTo)
                .orElseThrow();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterAccess(longestPeriod.multipliedBy(2))
                .build();
    }

    /** Uses one request of the client's budget, or throws {@link RateLimitExceededException}. */
    public void consume(RateLimitPolicy policy, String clientKey) {
        if (!enabled) {
            return;
        }
        Bucket bucket = buckets.get(policy.name() + ":" + clientKey, key -> policy.newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
            throw new RateLimitExceededException(policy, retryAfterSeconds);
        }
    }
}
