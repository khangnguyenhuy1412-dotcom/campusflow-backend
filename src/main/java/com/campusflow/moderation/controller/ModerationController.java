package com.campusflow.moderation.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.moderation.dto.ModerationNoteRequest;
import com.campusflow.moderation.dto.ReportDetailResponse;
import com.campusflow.moderation.dto.ResolveReportRequest;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.service.AccountBanService;
import com.campusflow.moderation.service.ModerationService;
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

/** Access to /api/moderation/** is limited to ROLE_MODERATOR and ROLE_ADMIN in SecurityConfig. */
@Tag(name = "Moderation", description = "Report queue and actions on listings and users (moderators)")
@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final AccountBanService accountBanService;

    @Operation(summary = "Report queue; status=OPEN shows the oldest first")
    @GetMapping("/reports")
    public PageResponse<ReportDetailResponse> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return moderationService.queue(status, page, size);
    }

    @Operation(summary = "Report details")
    @GetMapping("/reports/{id}")
    public ReportDetailResponse report(@PathVariable Long id) {
        return moderationService.get(id);
    }

    @Operation(summary = "Resolve a report (NONE, REMOVE_ITEM or BAN_USER); closes other open reports on the same target")
    @PostMapping("/reports/{id}/resolve")
    public ReportDetailResponse resolve(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request) {
        return moderationService.resolve(moderatorId, id, request);
    }

    @Operation(summary = "Dismiss a report that is not a violation")
    @PostMapping("/reports/{id}/dismiss")
    public ReportDetailResponse dismiss(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ModerationNoteRequest request) {
        return moderationService.dismiss(moderatorId, id, request.note());
    }

    @Operation(summary = "Remove a listing; its open deals are cancelled and the owner is told why")
    @PostMapping("/items/{id}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ModerationNoteRequest request) {
        moderationService.removeItem(moderatorId, id, request.note());
    }

    @Operation(summary = "Restore a removed listing as hidden")
    @PostMapping("/items/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreItem(@CurrentUserId Long moderatorId, @PathVariable Long id) {
        moderationService.restoreItem(moderatorId, id);
    }

    @Operation(summary = "Hide a false or abusive review; the reputation score is recalculated without it")
    @PostMapping("/reviews/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideReview(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ModerationNoteRequest request) {
        moderationService.hideReview(moderatorId, id, request.note());
    }

    @Operation(summary = "Show a hidden review again")
    @PostMapping("/reviews/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreReview(@CurrentUserId Long moderatorId, @PathVariable Long id) {
        moderationService.restoreReview(moderatorId, id);
    }

    @Operation(summary = "Hide a comment under a listing")
    @PostMapping("/comments/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideComment(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ModerationNoteRequest request) {
        moderationService.hideComment(moderatorId, id, request.note());
    }

    @Operation(summary = "Show a hidden comment again")
    @PostMapping("/comments/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreComment(@CurrentUserId Long moderatorId, @PathVariable Long id) {
        moderationService.restoreComment(moderatorId, id);
    }

    @Operation(summary = "Ban a user (moderators cannot ban moderators or admins)")
    @PostMapping("/users/{id}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void banUser(@CurrentUserId Long moderatorId, @PathVariable Long id,
            @Valid @RequestBody ModerationNoteRequest request) {
        accountBanService.ban(moderatorId, id, request.note());
    }
}
