package com.campusflow.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link RateLimit} annotations. Runs after Spring Security, so the user is already known.
 * Exceptions thrown here are turned into a 429 response by GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RateLimit rateLimit = method.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {
                rateLimiter.consume(rateLimit.policy(), clientKey(rateLimit.key(), request));
            }
        }
        return true;
    }

    private static String clientKey(RateLimit.Key key, HttpServletRequest request) {
        if (key == RateLimit.Key.USER) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return "user:" + jwt.getSubject();
            }
        }
        return "ip:" + request.getRemoteAddr();
    }
}
