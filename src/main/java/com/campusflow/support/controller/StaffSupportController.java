package com.campusflow.support.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.support.dto.StaffOpenTicketRequest;
import com.campusflow.support.dto.SupportMessageRequest;
import com.campusflow.support.dto.SupportMessageResponse;
import com.campusflow.support.dto.TicketResponse;
import com.campusflow.support.entity.TicketStatus;
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

/** Access to /api/moderation/** is limited to ROLE_MODERATOR and ROLE_ADMIN in SecurityConfig. */
@Tag(name = "Moderation - Support", description = "Support tickets from the staff side (moderators)")
@RestController
@RequestMapping("/api/moderation/support-tickets")
@RequiredArgsConstructor
public class StaffSupportController {

    private final SupportService supportService;

    @Operation(summary = "Ticket queue with unread counts; status=OPEN for active tickets")
    @GetMapping
    public PageResponse<TicketResponse> queue(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return supportService.queue(status, page, size);
    }

    @Operation(summary = "Contact a user (e.g. both sides of a disputed review); they are notified")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse open(@CurrentUserId Long staffId, @Valid @RequestBody StaffOpenTicketRequest request) {
        return supportService.openByStaff(staffId, request.userId(), request.subject(), request.message(),
                request.relatedReportId());
    }

    @Operation(summary = "Ticket details")
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return supportService.getForStaff(id);
    }

    @Operation(summary = "Messages (staff view shows which moderator wrote each message)")
    @GetMapping("/{id}/messages")
    public List<SupportMessageResponse> messages(@PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer size) {
        return supportService.messagesForStaff(id, before, size);
    }

    @Operation(summary = "Reply to the user; they get a notification and an email")
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportMessageResponse reply(@CurrentUserId Long staffId, @PathVariable Long id,
            @Valid @RequestBody SupportMessageRequest request) {
        return supportService.sendAsStaff(staffId, id, request.content());
    }

    @Operation(summary = "Mark the ticket as read for the whole team")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id) {
        supportService.markReadByStaff(id);
    }

    @Operation(summary = "Close the ticket")
    @PostMapping("/{id}/close")
    public TicketResponse close(@PathVariable Long id) {
        return supportService.closeByStaff(id);
    }
}
