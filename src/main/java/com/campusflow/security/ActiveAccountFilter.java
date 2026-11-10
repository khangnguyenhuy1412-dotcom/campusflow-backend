package com.campusflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests with a valid token from a banned (inactive) account. Runs right after the JWT
 * has been verified. Not a Spring bean on purpose, so it is only added to the security chain and
 * not registered a second time as a plain servlet filter.
 */
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final AccountStatusService accountStatusService;
    private final SecurityErrorHandler securityErrorHandler;

    public ActiveAccountFilter(AccountStatusService accountStatusService, SecurityErrorHandler securityErrorHandler) {
        this.accountStatusService = accountStatusService;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                && !accountStatusService.isActive(Long.valueOf(jwt.getSubject()))) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.commence(request, response, new DisabledException("Account is disabled"));
            return;
        }
        chain.doFilter(request, response);
    }
}
