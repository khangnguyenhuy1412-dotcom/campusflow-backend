package com.campusflow.notification.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.notification.dto.NotificationResponse;
import com.campusflow.notification.dto.UnreadCountResponse;
import com.campusflow.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's own notifications (login required, see SecurityConfig). */
@Tag(name = "Notifications", description = "In-app notifications of the logged-in user")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Your notifications, newest first")
    @GetMapping
    public PageResponse<NotificationResponse> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return notificationService.list(userId, unreadOnly, page, size);
    }

    @Operation(summary = "Number of unread notifications (for the bell badge)")
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@CurrentUserId Long userId) {
        return notificationService.unreadCount(userId);
    }

    @Operation(summary = "Mark one notification as read")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
    }

    @Operation(summary = "Mark all notifications as read")
    @PostMapping("/read-all")
    public UnreadCountResponse markAllRead(@CurrentUserId Long userId) {
        return notificationService.markAllRead(userId);
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        notificationService.delete(userId, id);
    }
}
