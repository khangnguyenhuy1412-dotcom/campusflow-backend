package com.campusflow.ratelimit;

import lombok.Getter;

/** Thrown when a client used up its budget; mapped to HTTP 429 with a Retry-After header. */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitPolicy policy;
    private final long retryAfterSeconds;

    public RateLimitExceededException(RateLimitPolicy policy, long retryAfterSeconds) {
        super("Rate limit exceeded for " + policy);
        this.policy = policy;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
