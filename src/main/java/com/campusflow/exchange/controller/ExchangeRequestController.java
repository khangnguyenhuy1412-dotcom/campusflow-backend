package com.campusflow.exchange.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.exchange.dto.CreateExchangeRequest;
import com.campusflow.exchange.dto.ExchangeRequestResponse;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.service.ExchangeService;
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
@RestController
@RequestMapping("/api/exchange-requests")
@RequiredArgsConstructor
public class ExchangeRequestController {

    private final ExchangeService exchangeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequestResponse create(@CurrentUserId Long userId, @Valid @RequestBody CreateExchangeRequest request) {
        return exchangeService.create(userId, request);
    }

    @GetMapping("/sent")
    public PageResponse<ExchangeRequestResponse> sent(@CurrentUserId Long userId,
            @RequestParam(required = false) ExchangeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return exchangeService.listSent(userId, status, page, size);
    }

    @GetMapping("/received")
    public PageResponse<ExchangeRequestResponse> received(@CurrentUserId Long userId,
            @RequestParam(required = false) ExchangeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return exchangeService.listReceived(userId, status, page, size);
    }

    @GetMapping("/{id}")
    public ExchangeRequestResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.get(userId, id);
    }

    @PostMapping("/{id}/accept")
    public ExchangeRequestResponse accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.accept(userId, id);
    }

    @PostMapping("/{id}/reject")
    public ExchangeRequestResponse reject(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.reject(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public ExchangeRequestResponse cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.cancel(userId, id);
    }

    @PostMapping("/{id}/complete")
    public ExchangeRequestResponse complete(@CurrentUserId Long userId, @PathVariable Long id) {
        return exchangeService.complete(userId, id);
    }
}
