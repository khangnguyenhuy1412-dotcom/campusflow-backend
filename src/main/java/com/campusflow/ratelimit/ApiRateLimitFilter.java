package com.campusflow.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Baseline per-IP limit for every /api request ({@link RateLimitPolicy#API}). Runs before Spring
 * Security, so floods are rejected before any token is parsed or database query is made.
 * <p>
 * The client IP is {@code request.getRemoteAddr()}. Behind a reverse proxy, set
 * {@code FORWARD_HEADERS_STRATEGY=native} so it comes from X-Forwarded-For; never trust that
 * header without a proxy, or clients could fake their IP to get a fresh budget.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final HandlerExceptionResolver resolver;

    public ApiRateLimitFilter(RateLimiter rateLimiter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.rateLimiter = rateLimiter;
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            rateLimiter.consume(RateLimitPolicy.API, "ip:" + request.getRemoteAddr());
        } catch (RateLimitExceededException e) {
            // Rendered by GlobalExceptionHandler, like every other error
            resolver.resolveException(request, response, null, e);
            return;
        }
        chain.doFilter(request, response);
    }
}
