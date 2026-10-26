package com.campusflow.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a {@link RateLimitPolicy} to a controller method; enforced by {@link RateLimitInterceptor}.
 * Example: {@code @RateLimit(policy = RateLimitPolicy.REGISTER, key = RateLimit.Key.IP)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    RateLimitPolicy policy();

    Key key();

    enum Key {
        /** Client IP address, for endpoints used before login. */
        IP,
        /** Logged-in user id (falls back to IP for anonymous requests). */
        USER
    }
}
