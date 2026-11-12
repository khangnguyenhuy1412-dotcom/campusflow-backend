package com.campusflow.support.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import com.campusflow.support.dto.OpenTicketRequest;
import com.campusflow.support.dto.SupportMessageRequest;
import com.campusflow.support.dto.SupportMessageResponse;
import com.campusflow.support.dto.TicketResponse;
import com.campusflow.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

/** Your own support tickets (login required). Live updates arrive on WebSocket /user/queue/support. */
@Tag(name = "Support", description = "Talk to the moderation team, e.g. about a disputed review or report")
@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @Operation(summary = "Open a ticket with the moderation team (at most 3 open)")
    @RateLimit(policy = RateLimitPolicy.CREATE_CONTENT, key = RateLimit.Key.USER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse open(@CurrentUserId Long userId, @Valid @RequestBody OpenTicketRequest request) {
        return supportService.openByUser(userId, request.subject(), request.message(), request.relatedReportId());
    }

    @Operation(summary = "Your tickets, most recently active first")
    @GetMapping
    public PageResponse<TicketResponse> mine(@CurrentUserId Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return supportService.listMine(userId, page, size);
    }

    @Operation(summary = "One of your tickets")
    @GetMapping("/{id}")
    public TicketResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return supportService.getMine(userId, id);
    }

    @Operation(summary = "Messages, newest first; pass before=<smallest id> for older ones")
    @GetMapping("/{id}/messages")
    public List<SupportMessageResponse> messages(@CurrentUserId Long userId, @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer size) {
        return supportService.messagesForUser(userId, id, before, size);
    }

    @Operation(summary = "Write to the moderation team")
    @RateLimit(policy = RateLimitPolicy.CHAT_MESSAGE, key = RateLimit.Key.USER)
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportMessageResponse send(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody SupportMessageRequest request) {
        return supportService.sendAsUser(userId, id, request.content());
    }

    @Operation(summary = "Mark the ticket as read")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        supportService.markReadByUser(userId, id);
    }

    @Operation(summary = "Close your ticket when the issue is solved")
    @PostMapping("/{id}/close")
    public TicketResponse close(@CurrentUserId Long userId, @PathVariable Long id) {
        return supportService.closeByUser(userId, id);
    }
}
