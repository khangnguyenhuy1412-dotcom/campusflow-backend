package com.campusflow.admin.controller;

import com.campusflow.admin.dto.AdminStatsResponse;
import com.campusflow.admin.dto.AdminUserResponse;
import com.campusflow.admin.dto.AdminUserStatus;
import com.campusflow.admin.dto.ModeratorRoleRequest;
import com.campusflow.admin.service.AdminStatsService;
import com.campusflow.admin.service.AdminUserService;
import com.campusflow.audit.dto.AuditLogResponse;
import com.campusflow.audit.service.AuditService;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.moderation.dto.ModerationNoteRequest;
import com.campusflow.moderation.service.AccountBanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Access to /api/admin/** is limited to ROLE_ADMIN in SecurityConfig. */
@Tag(name = "Admin", description = "Dashboard statistics, user management and audit log (ROLE_ADMIN)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AdminUserService adminUserService;
    private final AccountBanService accountBanService;
    private final AuditService auditService;

    @Operation(summary = "Dashboard numbers: totals, last 30 days, top categories and universities")
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminStatsService.stats();
    }

    @Operation(summary = "Search users by email or name, optionally only active or banned ones")
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) AdminUserStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adminUserService.search(query, status, page, size);
    }

    @Operation(summary = "Ban a user; takes effect immediately")
    @PostMapping("/users/{id}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ban(@CurrentUserId Long adminId, @PathVariable Long id, @Valid @RequestBody ModerationNoteRequest request) {
        accountBanService.ban(adminId, id, request.note());
    }

    @Operation(summary = "Lift a ban")
    @PostMapping("/users/{id}/unban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unban(@CurrentUserId Long adminId, @PathVariable Long id) {
        accountBanService.unban(adminId, id);
    }

    @Operation(summary = "Grant or remove the moderator role")
    @PutMapping("/users/{id}/moderator")
    public AdminUserResponse setModerator(@CurrentUserId Long adminId, @PathVariable Long id,
            @Valid @RequestBody ModeratorRoleRequest request) {
        return adminUserService.setModerator(adminId, id, request.moderator());
    }

    @Operation(summary = "Audit log of moderation and admin actions, newest first")
    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> auditLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return auditService.list(page, size);
    }
}
