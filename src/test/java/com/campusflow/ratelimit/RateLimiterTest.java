package com.campusflow.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter(new RateLimitProperties(true));

    private void useUp(RateLimitPolicy policy, String key) {
        for (int i = 0; i < policy.capacity(); i++) {
            limiter.consume(policy, key);
        }
    }

    @Test
    void allowsUpToCapacityThenRejectsWithRetryAfter() {
        useUp(RateLimitPolicy.PASSWORD_CHANGE, "user:1");

        assertThatThrownBy(() -> limiter.consume(RateLimitPolicy.PASSWORD_CHANGE, "user:1"))
                .isInstanceOfSatisfying(RateLimitExceededException.class, e -> {
                    assertThat(e.getPolicy()).isEqualTo(RateLimitPolicy.PASSWORD_CHANGE);
                    // One token refills every 15 min / 5 = 180 s
                    assertThat(e.getRetryAfterSeconds()).isBetween(1L, 181L);
                });
    }

    @Test
    void clientsHaveSeparateBudgets() {
        useUp(RateLimitPolicy.PASSWORD_CHANGE, "user:1");

        assertThatCode(() -> limiter.consume(RateLimitPolicy.PASSWORD_CHANGE, "user:2")).doesNotThrowAnyException();
    }

    @Test
    void policiesHaveSeparateBudgets() {
        useUp(RateLimitPolicy.LOGIN_EMAIL, "ip:1.2.3.4");

        assertThatCode(() -> limiter.consume(RateLimitPolicy.REGISTER, "ip:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void disabledLimiterNeverRejects() {
        RateLimiter disabled = new RateLimiter(new RateLimitProperties(false));

        assertThatCode(() -> {
            for (int i = 0; i < 1000; i++) {
                disabled.consume(RateLimitPolicy.PASSWORD_CHANGE, "user:1");
            }
        }).doesNotThrowAnyException();
    }
}
