package com.campusflow.exchange.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.exchange.dto.CreateExchangeRequest;
import com.campusflow.exchange.dto.ExchangeRequestResponse;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.service.ExchangeService;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** All endpoints need login; writes also need a verified student email (see SecurityConfig). */
@Tag(name = "Exchange requests", description = "Request, accept, reject, cancel and complete exchanges")
@RestController
@RequestMapping("/api/exchange-requests")
@RequiredArgsConstructor
public class ExchangeRequestController {

    private final ExchangeService exchangeService;

    @Operation(summary = "Request an item (EXCHANGE listings need an offered item)")
    @RateLimit(policy = RateLimitPolicy.CREATE_CONTENT, key = RateLimit.Key.USER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequestResponse create(@CurrentUserId Long userId, @Valid @RequestBody CreateExchangeRequest request) {
        return exchangeService.create(userId, request);
    }

    @Operation(summary = "Requests you sent")
    @GetMapping("/sent")
    public PageResponse<ExchangeRequestResponse> sent(@CurrentUserId Long userId,
            @RequestParam(required = false) ExchangeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return exchangeService.listSent(userId, status, page, size);
    }

    @Operation(summary = "Requests for your items")
    @GetMapping("/received")
    public PageResponse<ExchangeRequestResponse> received(@CurrentUserId Long userId,
            @RequestParam(required = false) ExchangeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return exchangeService.listReceived(userId, status, page, size);
    }

    @Operation(summary = "Request details (participants only)")
    @GetMapping("/{id}")
    public ExchangeRequestResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.get(userId, id);
    }

    @Operation(summary = "Owner accepts; both items are reserved and contacts shared")
    @PostMapping("/{id}/accept")
    public ExchangeRequestResponse accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.accept(userId, id);
    }

    @Operation(summary = "Owner rejects a pending request")
    @PostMapping("/{id}/reject")
    public ExchangeRequestResponse reject(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.reject(userId, id);
    }

    @Operation(summary = "Cancel: requester (pending/accepted) or owner (accepted)")
    @PostMapping("/{id}/cancel")
    public ExchangeRequestResponse cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.cancel(userId, id);
    }

    @Operation(summary = "Owner confirms the hand-over")
    @PostMapping("/{id}/complete")
    public ExchangeRequestResponse complete(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.complete(userId, id);
    }
}
